package com.ist.internal_issue_tracker.user;


import com.ist.internal_issue_tracker.shared.exception.DuplicateResourceException;
import com.ist.internal_issue_tracker.user.exception.UserErrorCode;
import com.ist.internal_issue_tracker.user.dto.UserCreateRequest;
import com.ist.internal_issue_tracker.user.dto.UserResponse;
import com.ist.internal_issue_tracker.user.internal.PasswordHasher;
import com.ist.internal_issue_tracker.user.mapper.UserMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordHasher passwordHasher;

    public UserResponse createUser(UserCreateRequest request) {

        // email unique check
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException(UserErrorCode.EMAIL_ALREADY_EXISTS,
                    "This e-mail is already exists: " + request.email());
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
            throw new DuplicateResourceException(UserErrorCode.EMAIL_ALREADY_EXISTS,
                    "This e-mail is already exists: " + request.email());
        }

        // return user response
        return userMapper.toResponse(savedUser);
    }
}
