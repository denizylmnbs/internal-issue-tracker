package com.ist.internal_issue_tracker.user.mapper;

import com.ist.internal_issue_tracker.user.User;
import com.ist.internal_issue_tracker.user.dto.UserCreateRequest;
import com.ist.internal_issue_tracker.user.dto.UserResponse;
import com.ist.internal_issue_tracker.user.dto.UserUpdateRequest;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

  public User toEntity(UserCreateRequest request) {
    User user = new User();

    user.setName(request.name());
    user.setSurname(request.surname());
    user.setEmail(request.email());

    return user;
  }

  public void updateEntity(User user, UserUpdateRequest request) {
    user.setName(request.name());
    user.setSurname(request.surname());
    user.setEmail(request.email());
  }

  public UserResponse toResponse(User user) {
    return new UserResponse(
        user.getId(),
        user.getName(),
        user.getSurname(),
        user.getEmail(),
        user.getIsAdmin(),
        user.getIsActive(),
        user.getCreatedAt());
  }
}
