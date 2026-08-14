package com.ist.internal_issue_tracker.user.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.ist.internal_issue_tracker.shared.storage.ObjectUrlSigner;
import com.ist.internal_issue_tracker.user.User;
import com.ist.internal_issue_tracker.user.dto.UserResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserMapperTest {

  @Mock private ObjectUrlSigner objectUrlSigner;
  @InjectMocks private UserMapper userMapper;

  @Test
  void toResponse_leavesAvatarUrlNull_whenUserHasNoAvatar() {
    User user = new User();
    user.setName("Ada");
    user.setSurname("Lovelace");
    user.setEmail("ada@ist.com");

    UserResponse response = userMapper.toResponse(user);

    assertThat(response.avatarUrl()).isNull();
    verifyNoInteractions(objectUrlSigner);
  }

  @Test
  void toResponse_signsTheStoredKey_whenUserHasAnAvatar() {
    User user = new User();
    user.setAvatarObjectKey("avatars/1/some-uuid");
    when(objectUrlSigner.presignedGetUrl("avatars/1/some-uuid"))
        .thenReturn("https://minio.example/avatars/1/some-uuid?signature=abc");

    UserResponse response = userMapper.toResponse(user);

    assertThat(response.avatarUrl())
        .isEqualTo("https://minio.example/avatars/1/some-uuid?signature=abc");
    verify(objectUrlSigner).presignedGetUrl("avatars/1/some-uuid");
  }
}
