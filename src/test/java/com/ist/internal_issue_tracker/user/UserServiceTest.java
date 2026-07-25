package com.ist.internal_issue_tracker.user;

import com.ist.internal_issue_tracker.shared.exception.DuplicateResourceException;
import com.ist.internal_issue_tracker.user.dto.UserCreateRequest;
import com.ist.internal_issue_tracker.user.dto.UserResponse;
import com.ist.internal_issue_tracker.user.exception.UserErrorCode;
import com.ist.internal_issue_tracker.user.internal.PasswordHasher;
import com.ist.internal_issue_tracker.user.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordHasher passwordHasher;

    @InjectMocks
    private UserService userService;

    private static final UserCreateRequest REQUEST =
            new UserCreateRequest("Ada", "Lovelace", "ada@ist.com", "password123");

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
        when(userRepository.save(entity)).thenThrow(new DataIntegrityViolationException("duplicate key"));

        assertThatThrownBy(() -> userService.createUser(REQUEST))
                .isInstanceOf(DuplicateResourceException.class)
                .extracting(ex -> ((DuplicateResourceException) ex).errorCode())
                .isEqualTo(UserErrorCode.EMAIL_ALREADY_EXISTS);
    }
}
