package com.ist.internal_issue_tracker.user;


import com.ist.internal_issue_tracker.shared.exception.DuplicateResourceException;
import com.ist.internal_issue_tracker.user.exception.UserErrorCode;
import com.ist.internal_issue_tracker.user.dto.UserCreateRequest;
import com.ist.internal_issue_tracker.user.dto.UserResponse;
import com.ist.internal_issue_tracker.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public UserResponse createUser(UserCreateRequest request) {

        // email unique check
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException(UserErrorCode.EMAIL_ALREADY_EXISTS,
                    "This e-mail is already exist: " + request.email());
        }

    }
}
