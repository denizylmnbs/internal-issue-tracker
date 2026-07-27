package com.ist.internal_issue_tracker.user;


import com.ist.internal_issue_tracker.shared.exception.AppException;
import com.ist.internal_issue_tracker.shared.exception.DuplicateResourceException;
import com.ist.internal_issue_tracker.shared.exception.ResourceNotFoundException;
import com.ist.internal_issue_tracker.shared.security.AuthenticatedUser;
import com.ist.internal_issue_tracker.shared.web.PagedResponse;
import com.ist.internal_issue_tracker.user.dto.ChangePasswordRequest;
import com.ist.internal_issue_tracker.user.dto.ResetPasswordRequest;
import com.ist.internal_issue_tracker.user.dto.UserUpdateRequest;
import com.ist.internal_issue_tracker.user.exception.UserErrorCode;
import com.ist.internal_issue_tracker.user.dto.UserCreateRequest;
import com.ist.internal_issue_tracker.user.dto.UserResponse;
import com.ist.internal_issue_tracker.user.internal.PasswordHasher;
import com.ist.internal_issue_tracker.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordHasher passwordHasher;

    /** Used by the {@code auth} module to authenticate a login attempt without exposing password hashing internals. */
    public AuthenticatedUser verifyCredentials(String email, String rawPassword) {
        User user = userRepository.findByEmail(email)
                .filter(User::getIsActive)
                .orElseThrow(() -> new AppException(UserErrorCode.INVALID_CREDENTIALS));

        if (!passwordHasher.matches(rawPassword, user.getPasswordHashed())) {
            throw new AppException(UserErrorCode.INVALID_CREDENTIALS);
        }

        return new AuthenticatedUser(user.getId(), user.getIsAdmin());
    }

    private DuplicateResourceException emailAlreadyExists(String email) {
        return new DuplicateResourceException(UserErrorCode.EMAIL_ALREADY_EXISTS,
                "This email already exists: " + email);
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
        User user = userRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("User", id));

        return userMapper.toResponse(user);
    }

    public PagedResponse<UserResponse> getAllUsers(String name, String surname, Pageable pageable) {
        Page<User> users = userRepository.findAllByFilters(name, surname, pageable);
        Page<UserResponse> responsePage = users.map(user -> userMapper.toResponse(user));

        return PagedResponse.from(responsePage);
    }

    public UserResponse updateUser(Integer id, UserUpdateRequest request) {

        // fetch existing user
        User user = userRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("User", id));

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

    public void deleteUser(Integer id) {
        // fetch existing user
        User user = userRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("User", id));

        user.setIsActive(false);
    }

    public UserResponse changePassword(Integer id, ChangePasswordRequest request) {
        // authorization (self-or-admin) is enforced in UserController via @PreAuthorize
        // fetch existing user
        User user = userRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("User", id));

        // current password check
        if (!passwordHasher.matches(request.currentPassword(), user.getPasswordHashed())) {
            throw new AppException(UserErrorCode.CURRENT_PASSWORD_INCORRECT);
        }

        user.changePassword(passwordHasher.hash(request.newPassword()));

        return userMapper.toResponse(user);
    }

    public void resetPassword(Integer id, ResetPasswordRequest request) {
        // authorization (admin-only) is enforced in UserController via @PreAuthorize
        // fetch existing user
        User user = userRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("User", id));

        user.changePassword(passwordHasher.hash(request.newPassword()));
    }
}
