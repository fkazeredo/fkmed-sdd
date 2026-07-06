package com.fkmed.domain.notification;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * SPEC-0004 BR4 / AC5 content guard: no notification template ({@code notification.*} message) may
 * embed full sensitive data — CPF/CNS/bank numbers. Monetary values (e.g. {@code R$ 120,00}) are
 * allowed because they have no long digit run. Applies to every wired producer's title/body plus
 * the catalog labels, so a future template that pastes raw sensitive data fails this gate.
 */
class NotificationContentMaskingTest {

  // Any run of 11+ digits catches an unmasked CPF (11), CNS (15) or bank account number.
  private static final Pattern LONG_DIGIT_RUN = Pattern.compile("\\d{11,}");
  private static final Pattern FORMATTED_CPF = Pattern.compile("\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}");

  @Test
  void noNotificationMessageEmbedsFullSensitiveData() throws Exception {
    Properties bundle = new Properties();
    try (var stream = getClass().getResourceAsStream("/messages.properties")) {
      assertThat(stream).as("messages.properties must exist").isNotNull();
      bundle.load(new InputStreamReader(stream, StandardCharsets.UTF_8));
    }

    var notificationMessages =
        bundle.stringPropertyNames().stream()
            .filter(key -> key.startsWith("notification."))
            .toList();
    assertThat(notificationMessages)
        .as("the notification templates must be present in the bundle")
        .isNotEmpty();

    for (String key : notificationMessages) {
      String value = bundle.getProperty(key);
      assertThat(LONG_DIGIT_RUN.matcher(value).find())
          .as(
              "notification message '%s' must not embed an 11+ digit run (CPF/CNS/bank): %s",
              key, value)
          .isFalse();
      assertThat(FORMATTED_CPF.matcher(value).find())
          .as("notification message '%s' must not embed a formatted CPF: %s", key, value)
          .isFalse();
    }
  }
}
