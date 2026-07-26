package com.ist.internal_issue_tracker.user;


import com.ist.internal_issue_tracker.shared.exception.DuplicateResourceException;
import com.ist.internal_issue_tracker.shared.exception.ResourceNotFoundException;
import com.ist.internal_issue_tracker.shared.web.PagedResponse;
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

    public PagedResponse<UserResponse> getAllUsers(Pageable pageable) {
        Page<User> users = userRepository.findAll(pageable);
        Page<UserResponse> responsePage = users.map(user -> userMapper.toResponse(user));

        return PagedResponse.from(responsePage);
    }
}
