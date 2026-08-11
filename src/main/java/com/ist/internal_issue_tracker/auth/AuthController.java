package com.ist.internal_issue_tracker.auth;

import com.ist.internal_issue_tracker.auth.dto.LoginRequest;
import com.ist.internal_issue_tracker.auth.dto.LoginResponse;
import com.ist.internal_issue_tracker.auth.dto.RefreshTokenRequest;
import com.ist.internal_issue_tracker.shared.exception.AppException;
import com.ist.internal_issue_tracker.shared.exception.CommonErrorCode;
import com.ist.internal_issue_tracker.shared.ratelimit.RateLimiterService;
import com.ist.internal_issue_tracker.shared.web.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;
  private final RateLimiterService rateLimiterService;

  @PostMapping("/login")
  public ResponseEntity<ApiResponse<LoginResponse>> login(
      @Valid @RequestBody LoginRequest request) {
    // IP bazlı limit RateLimitFilter'da; bu, dağıtık IP'lerden aynı hesaba yapılan
    // credential-stuffing denemelerini yakalamak için ayrıca hesap (email) bazlı çalışır.
    if (!rateLimiterService.tryConsume(
        "account:login:" + request.email(), RateLimiterService.perAccount())) {
      throw new AppException(CommonErrorCode.RATE_LIMITED);
    }

    LoginResponse loginResponse = authService.login(request);

    return ResponseEntity.ok(ApiResponse.ok(loginResponse));
  }

  @PostMapping("/refresh")
  public ResponseEntity<ApiResponse<LoginResponse>> refresh(
      @Valid @RequestBody RefreshTokenRequest request) {
    LoginResponse loginResponse = authService.refresh(request);

    return ResponseEntity.ok(ApiResponse.ok(loginResponse));
  }

  @PostMapping("/logout")
  public ResponseEntity<ApiResponse<Void>> logout(@Valid @RequestBody RefreshTokenRequest request) {
    authService.logout(request);

    return ResponseEntity.ok(ApiResponse.ok());
  }
}
