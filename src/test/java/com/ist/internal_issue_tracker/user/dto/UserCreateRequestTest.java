package com.ist.internal_issue_tracker.user.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class UserCreateRequestTest {

  @Test
  void normalizesEmail_trimAndLowerCase() {
    UserCreateRequest request =
        new UserCreateRequest("Ada", "Lovelace", "  Ada@IST.com  ", "password123");

    assertThat(request.email()).isEqualTo("ada@ist.com");
  }
}
