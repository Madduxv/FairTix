package com.fairtix.auth.application;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RecaptchaServiceTest {

  private static final String SECRET = "test-secret";

  private RecaptchaService enabled(RestTemplate restTemplate) {
    return new RecaptchaService(restTemplate, true, 3, SECRET);
  }

  private RecaptchaService disabled() {
    return new RecaptchaService(mock(RestTemplate.class), false, 3, SECRET);
  }

  // -- isEnabled / isCaptchaRequired ----------------------------------------

  @Test
  void isEnabled_returnsFalse_whenDisabled() {
    assertThat(disabled().isEnabled()).isFalse();
  }

  @Test
  void isEnabled_returnsFalse_whenSecretBlank() {
    var svc = new RecaptchaService(mock(RestTemplate.class), true, 3, "");
    assertThat(svc.isEnabled()).isFalse();
  }

  @Test
  void isEnabled_returnsTrue_whenEnabledWithSecret() {
    assertThat(enabled(mock(RestTemplate.class)).isEnabled()).isTrue();
  }

  @Test
  void isCaptchaRequired_respectsThreshold() {
    var svc = enabled(mock(RestTemplate.class));
    assertThat(svc.isCaptchaRequired(0)).isFalse();
    assertThat(svc.isCaptchaRequired(2)).isFalse();
    assertThat(svc.isCaptchaRequired(3)).isTrue();
    assertThat(svc.isCaptchaRequired(10)).isTrue();
  }

  @Test
  void isCaptchaRequired_alwaysFalse_whenDisabled() {
    assertThat(disabled().isCaptchaRequired(100)).isFalse();
  }

  // -- assertValidToken: disabled mode --------------------------------------

  @Test
  void assertValidToken_skips_whenDisabled() {
    // Should not throw even with null token
    disabled().assertValidToken(null);
    disabled().assertValidToken("");
    disabled().assertValidToken("any-token");
  }

  // -- assertValidToken: enabled mode, blank/null token ---------------------

  @Test
  void assertValidToken_throwsCaptchaRequired_whenTokenNull() {
    assertThatThrownBy(() -> enabled(mock(RestTemplate.class)).assertValidToken(null))
        .isInstanceOf(CaptchaRequiredException.class);
  }

  @Test
  void assertValidToken_throwsCaptchaRequired_whenTokenBlank() {
    assertThatThrownBy(() -> enabled(mock(RestTemplate.class)).assertValidToken("  "))
        .isInstanceOf(CaptchaRequiredException.class);
  }

  // -- assertValidToken: enabled mode, verification outcomes ----------------

  @Test
  void assertValidToken_succeeds_whenGoogleReturnsSuccess() {
    RestTemplate rest = mock(RestTemplate.class);
    when(rest.postForEntity(anyString(), any(), eq(Map.class)))
        .thenReturn(ResponseEntity.ok(Map.of("success", true)));

    // Should not throw
    enabled(rest).assertValidToken("valid-token");
  }

  @Test
  void assertValidToken_throwsInvalidCaptcha_whenGoogleReturnsFalse() {
    RestTemplate rest = mock(RestTemplate.class);
    when(rest.postForEntity(anyString(), any(), eq(Map.class)))
        .thenReturn(ResponseEntity.ok(Map.of("success", false)));

    assertThatThrownBy(() -> enabled(rest).assertValidToken("bad-token"))
        .isInstanceOf(InvalidCaptchaException.class);
  }

  @Test
  void assertValidToken_throwsInvalidCaptcha_whenResponseBodyNull() {
    RestTemplate rest = mock(RestTemplate.class);
    when(rest.postForEntity(anyString(), any(), eq(Map.class)))
        .thenReturn(ResponseEntity.ok(null));

    assertThatThrownBy(() -> enabled(rest).assertValidToken("token"))
        .isInstanceOf(InvalidCaptchaException.class);
  }

  // -- assertValidToken: network failure ------------------------------------

  @Test
  void assertValidToken_throwsRecaptchaUnavailable_whenApiUnreachable() {
    RestTemplate rest = mock(RestTemplate.class);
    when(rest.postForEntity(anyString(), any(), eq(Map.class)))
        .thenThrow(new ResourceAccessException("Connection refused"));

    assertThatThrownBy(() -> enabled(rest).assertValidToken("token"))
        .isInstanceOf(RecaptchaUnavailableException.class)
        .hasCauseInstanceOf(ResourceAccessException.class);
  }

  @Test
  void assertValidToken_throwsInvalidCaptcha_onOtherException() {
    RestTemplate rest = mock(RestTemplate.class);
    when(rest.postForEntity(anyString(), any(), eq(Map.class)))
        .thenThrow(new RuntimeException("unexpected"));

    assertThatThrownBy(() -> enabled(rest).assertValidToken("token"))
        .isInstanceOf(InvalidCaptchaException.class);
  }
}
