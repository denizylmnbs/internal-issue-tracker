package com.ist.internal_issue_tracker.user;

import com.ist.internal_issue_tracker.shared.event.UserCredentialsChangedEvent;
import com.ist.internal_issue_tracker.shared.event.UserDeactivatedEvent;
import com.ist.internal_issue_tracker.shared.event.UserRoleChangedEvent;
import com.ist.internal_issue_tracker.shared.exception.AppException;
import com.ist.internal_issue_tracker.shared.exception.DuplicateResourceException;
import com.ist.internal_issue_tracker.shared.exception.ResourceNotFoundException;
import com.ist.internal_issue_tracker.shared.security.AuthenticatedUser;
import com.ist.internal_issue_tracker.shared.security.Role;
import com.ist.internal_issue_tracker.shared.web.PagedResponse;
import com.ist.internal_issue_tracker.user.dto.*;
import com.ist.internal_issue_tracker.user.exception.UserErrorCode;
import com.ist.internal_issue_tracker.user.internal.PasswordHasher;
import com.ist.internal_issue_tracker.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;
  private final UserMapper userMapper;
  private final PasswordHasher passwordHasher;
  private final ApplicationEventPublisher eventPublisher;

  /**
   * Authenticates a login attempt without exposing password hashing internals. Reached by {@code
   * auth} through {@link UserCredentialsVerifierAdapter}, never directly.
   */
  public AuthenticatedUser verifyCredentials(String email, String rawPassword) {
    User user =
        userRepository
            .findByEmail(email)
            .filter(User::getIsActive)
            .orElseThrow(() -> new AppException(UserErrorCode.INVALID_CREDENTIALS));

    if (!passwordHasher.matches(rawPassword, user.getPasswordHashed())) {
      throw new AppException(UserErrorCode.INVALID_CREDENTIALS);
    }

    return new AuthenticatedUser(user.getId(), user.getRole());
  }

  private DuplicateResourceException emailAlreadyExists(String email) {
    return new DuplicateResourceException(
        UserErrorCode.EMAIL_ALREADY_EXISTS, "This email already exists: " + email);
  }

  public UserResponse createUser(UserCreateRequest request) {

    // email unique check
    if (userRepository.existsByEmail(request.email())) {
      throw emailAlreadyExists(request.email());
    }

    // password hashing
    String hashedPassword = passwordHasher.hash(request.password());

    // map to entity
    User user = userMapper.toEntity(request);
    user.changePassword(hashedPassword);

    // save to db and prevent race conditions
    User savedUser;
    try {
      savedUser = userRepository.save(user);
    } catch (DataIntegrityViolationException e) {
      // only unique constraint on User currently is email, revisit if a second one is added
      throw emailAlreadyExists(request.email());
    }

    // return user response
    return userMapper.toResponse(savedUser);
  }

  public UserResponse getUserById(Integer id) {
    User user =
        userRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("User", id));

    return userMapper.toResponse(user);
  }

  public PagedResponse<UserResponse> getAllUsers(String name, String surname, Pageable pageable) {
    Page<User> users = userRepository.findAllByFilters(name, surname, pageable);
    Page<UserResponse> responsePage = users.map(user -> userMapper.toResponse(user));

    return PagedResponse.from(responsePage);
  }

  public UserResponse updateUser(Integer id, UserUpdateRequest request) {

    // fetch existing user
    User user =
        userRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("User", id));

    // email unique check
    if (userRepository.existsByEmailAndIdNot(request.email(), id)) {
      throw emailAlreadyExists(request.email());
    }

    // apply changes to the managed entity
    userMapper.updateEntity(user, request);

    // save to db and prevent race conditions
    User savedUser;
    try {
      savedUser = userRepository.save(user);
    } catch (DataIntegrityViolationException e) {
      // only unique constraint on User currently is email, revisit if a second one is added
      throw emailAlreadyExists(request.email());
    }

    return userMapper.toResponse(savedUser);
  }

  /**
   * Soft-deletes the user and tells the rest of the application to retire whatever points at them.
   * {@code team} and {@code project} listen for the event and clear their own membership rows,
   * which is what lets their roster queries trust {@code is_active} on the row instead of joining
   * back to {@code users}.
   *
   * <p>Transactional because the two have to move together: a user marked inactive while their
   * memberships stayed live would put them on rosters they are no longer part of.
   */
  @Transactional
  public void deleteUser(Integer id) {
    // fetch existing user
    User user =
        userRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("User", id));

    user.setIsActive(false);

    userRepository.save(user);

    eventPublisher.publishEvent(new UserDeactivatedEvent(id));
  }

  public UserResponse changePassword(Integer id, ChangePasswordRequest request) {
    // authorization (self-or-admin) is enforced in UserController via @PreAuthorize
    // fetch existing user
    User user =
        userRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("User", id));

    // current password check
    if (!passwordHasher.matches(request.currentPassword(), user.getPasswordHashed())) {
      throw new AppException(UserErrorCode.CURRENT_PASSWORD_INCORRECT);
    }

    user.changePassword(passwordHasher.hash(request.newPassword()));

    UserResponse response = userMapper.toResponse(userRepository.save(user));
    eventPublisher.publishEvent(new UserCredentialsChangedEvent(id));

    return response;
  }

  public void resetPassword(Integer id, ResetPasswordRequest request) {
    // authorization (admin-only) is enforced in UserController via @PreAuthorize
    // fetch existing user
    User user =
        userRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("User", id));

    user.changePassword(passwordHasher.hash(request.newPassword()));

    userRepository.save(user);
    eventPublisher.publishEvent(new UserCredentialsChangedEvent(id));
  }

  /**
   * {@code @Transactional} so the save and the event move together - {@link
   * AuthPrincipalCacheEvictionListener} and {@code auth}'s refresh-token revocation both react to
   * {@link UserRoleChangedEvent} synchronously, and neither should fire off the back of a role
   * change that then fails to commit.
   */
  @Transactional
  public UserResponse changeRole(Integer id, RoleChangeRequest request, AuthenticatedUser caller) {
    // fetch existing user
    User user =
        userRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("User", id));

    // variable definitions
    Role userRole = user.getRole();
    Role callerRole = caller.getRole();
    Role newRole = request.newRole();

    // authorization check
    if (!callerRole.outranks(userRole) || !callerRole.outranks(newRole)) {
      throw new AppException(UserErrorCode.ROLE_CHANGE_NOT_PERMITTED);
    }
    user.changeRole(newRole);

    UserResponse response = userMapper.toResponse(userRepository.save(user));
    eventPublisher.publishEvent(new UserRoleChangedEvent(id));

    return response;
  }
}
