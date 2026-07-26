package com.ist.internal_issue_tracker.user;

import com.ist.internal_issue_tracker.shared.web.ApiResponse;
import com.ist.internal_issue_tracker.user.dto.UserCreateRequest;
import com.ist.internal_issue_tracker.user.dto.UserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> createUser(@Valid @RequestBody UserCreateRequest request) {

        UserResponse userResponse = userService.createUser(request);

        return ResponseEntity.created(URI.create("/api/users/" + userResponse.id())).body(ApiResponse.ok(userResponse));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Integer id) {
        UserResponse userResponse = userService.getUserById(id);

        return ResponseEntity.ok(ApiResponse.ok(userResponse));
    }
}
