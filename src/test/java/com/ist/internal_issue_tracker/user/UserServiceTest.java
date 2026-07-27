package com.ist.internal_issue_tracker.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ist.internal_issue_tracker.shared.exception.DuplicateResourceException;
import com.ist.internal_issue_tracker.shared.exception.ResourceNotFoundException;
import com.ist.internal_issue_tracker.shared.web.PagedResponse;
import com.ist.internal_issue_tracker.user.dto.UserCreateRequest;
import com.ist.internal_issue_tracker.user.dto.UserResponse;
import com.ist.internal_issue_tracker.user.dto.UserUpdateRequest;
import com.ist.internal_issue_tracker.user.exception.UserErrorCode;
import com.ist.internal_issue_tracker.user.internal.PasswordHasher;
import com.ist.internal_issue_tracker.user.mapper.UserMapper;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  private static final UserCreateRequest REQUEST =
      new UserCreateRequest("Ada", "Lovelace", "ada@ist.com", "password123");
  @Mock private UserRepository userRepository;
  @Mock private UserMapper userMapper;
  @Mock private PasswordHasher passwordHasher;
  @InjectMocks private UserService userService;

  @Test
  void createUser_savesUser_whenEmailIsUnique() {
    User entity = new User();
    User savedEntity = new User();
    UserResponse expectedResponse =
        new UserResponse(1, "Ada", "Lovelace", "ada@ist.com", false, true, OffsetDateTime.now());

    when(userRepository.existsByEmail(REQUEST.email())).thenReturn(false);
    when(passwordHasher.hash(REQUEST.password())).thenReturn("hashed-password");
    when(userMapper.toEntity(REQUEST)).thenReturn(entity);
    when(userRepository.save(entity)).thenReturn(savedEntity);
    when(userMapper.toResponse(savedEntity)).thenReturn(expectedResponse);

    UserResponse actualResponse = userService.createUser(REQUEST);

    assertThat(actualResponse).isEqualTo(expectedResponse);
    verify(userRepository).save(entity);
  }

  @Test
  void createUser_throwsDuplicateResourceException_whenEmailAlreadyExists() {
    when(userRepository.existsByEmail(REQUEST.email())).thenReturn(true);

    assertThatThrownBy(() -> userService.createUser(REQUEST))
        .isInstanceOf(DuplicateResourceException.class)
        .extracting(ex -> ((DuplicateResourceException) ex).errorCode())
        .isEqualTo(UserErrorCode.EMAIL_ALREADY_EXISTS);

    verify(passwordHasher, never()).hash(any());
    verify(userRepository, never()).save(any());
  }

  @Test
  void createUser_throwsDuplicateResourceException_whenSaveRaceLosesToUniqueConstraint() {
    User entity = new User();

    when(userRepository.existsByEmail(REQUEST.email())).thenReturn(false);
    when(passwordHasher.hash(REQUEST.password())).thenReturn("hashed-password");
    when(userMapper.toEntity(REQUEST)).thenReturn(entity);
    when(userRepository.save(entity))
        .thenThrow(new DataIntegrityViolationException("duplicate key"));

    assertThatThrownBy(() -> userService.createUser(REQUEST))
        .isInstanceOf(DuplicateResourceException.class)
        .extracting(ex -> ((DuplicateResourceException) ex).errorCode())
        .isEqualTo(UserErrorCode.EMAIL_ALREADY_EXISTS);
  }

  @Test
  void getUserById_returnsUser_whenExists() {
    User entity = new User();
    UserResponse expectedResponse =
        new UserResponse(1, "Ada", "Lovelace", "ada@ist.com", false, true, OffsetDateTime.now());

    when(userRepository.findById(1)).thenReturn(Optional.of(entity));
    when(userMapper.toResponse(entity)).thenReturn(expectedResponse);

    UserResponse actualResponse = userService.getUserById(1);

    assertThat(actualResponse).isEqualTo(expectedResponse);
  }

  @Test
  void getUserById_throwsResourceNotFoundException_whenNotFound() {
    when(userRepository.findById(1)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> userService.getUserById(1))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void getAllUsers_returnsPagedResponse() {
    User entity = new User();
    UserResponse response =
        new UserResponse(1, "Ada", "Lovelace", "ada@ist.com", false, true, OffsetDateTime.now());
    Pageable pageable = PageRequest.of(0, 20);

    when(userRepository.findAllByFilters("Ada", null, pageable))
        .thenReturn(new PageImpl<>(List.of(entity), pageable, 1));
    when(userMapper.toResponse(entity)).thenReturn(response);

    PagedResponse<UserResponse> actualResponse = userService.getAllUsers("Ada", null, pageable);

    assertThat(actualResponse.content()).containsExactly(response);
    assertThat(actualResponse.page().totalElements()).isEqualTo(1);
  }

  @Test
  void updateUser_updatesUser_whenEmailIsUnique() {
    UserUpdateRequest request = new UserUpdateRequest("Grace", "Hopper", "grace@ist.com");
    User entity = new User();
    User savedEntity = new User();
    UserResponse expectedResponse =
        new UserResponse(1, "Grace", "Hopper", "grace@ist.com", false, true, OffsetDateTime.now());

    when(userRepository.findById(1)).thenReturn(Optional.of(entity));
    when(userRepository.existsByEmailAndIdNot(request.email(), 1)).thenReturn(false);
    when(userRepository.save(entity)).thenReturn(savedEntity);
    when(userMapper.toResponse(savedEntity)).thenReturn(expectedResponse);

    UserResponse actualResponse = userService.updateUser(1, request);

    assertThat(actualResponse).isEqualTo(expectedResponse);
    verify(userMapper).updateEntity(entity, request);
    verify(userRepository).save(entity);
  }

  @Test
  void updateUser_throwsResourceNotFoundException_whenUserNotFound() {
    UserUpdateRequest request = new UserUpdateRequest("Grace", "Hopper", "grace@ist.com");

    when(userRepository.findById(1)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> userService.updateUser(1, request))
        .isInstanceOf(ResourceNotFoundException.class);

    verify(userRepository, never()).save(any());
  }

  @Test
  void updateUser_throwsDuplicateResourceException_whenEmailAlreadyExistsForAnotherUser() {
    UserUpdateRequest request = new UserUpdateRequest("Grace", "Hopper", "grace@ist.com");
    User entity = new User();

    when(userRepository.findById(1)).thenReturn(Optional.of(entity));
    when(userRepository.existsByEmailAndIdNot(request.email(), 1)).thenReturn(true);

    assertThatThrownBy(() -> userService.updateUser(1, request))
        .isInstanceOf(DuplicateResourceException.class)
        .extracting(ex -> ((DuplicateResourceException) ex).errorCode())
        .isEqualTo(UserErrorCode.EMAIL_ALREADY_EXISTS);

    verify(userRepository, never()).save(any());
  }

  @Test
  void updateUser_throwsDuplicateResourceException_whenSaveRaceLosesToUniqueConstraint() {
    UserUpdateRequest request = new UserUpdateRequest("Grace", "Hopper", "grace@ist.com");
    User entity = new User();

    when(userRepository.findById(1)).thenReturn(Optional.of(entity));
    when(userRepository.existsByEmailAndIdNot(request.email(), 1)).thenReturn(false);
    when(userRepository.save(entity))
        .thenThrow(new DataIntegrityViolationException("duplicate key"));

    assertThatThrownBy(() -> userService.updateUser(1, request))
        .isInstanceOf(DuplicateResourceException.class)
        .extracting(ex -> ((DuplicateResourceException) ex).errorCode())
        .isEqualTo(UserErrorCode.EMAIL_ALREADY_EXISTS);
  }

  @Test
  void deleteUser_deactivatesUser_whenExists() {
    User entity = new User();
    entity.setIsActive(true);

    when(userRepository.findById(1)).thenReturn(Optional.of(entity));

    userService.deleteUser(1);

    assertThat(entity.getIsActive()).isFalse();
    verify(userRepository, never()).save(any());
  }

  @Test
  void deleteUser_throwsResourceNotFoundException_whenNotFound() {
    when(userRepository.findById(1)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> userService.deleteUser(1))
        .isInstanceOf(ResourceNotFoundException.class);
  }
}
