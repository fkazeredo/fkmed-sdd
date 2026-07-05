package com.fkmed.domain.notification;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * SPEC-0004 BR7 e-mail-channel resolution (default × opt-out × mandatory). In-app is always on and
 * is not governed by this rule.
 */
class NotificationEventTypeTest {

  @Test
  void nonMandatoryDefaultOn_emailsUnlessOptedOut() {
    assertThat(NotificationEventType.emailEnabled(false, true, false)).isTrue();
    assertThat(NotificationEventType.emailEnabled(false, true, true)).isFalse();
  }

  @Test
  void nonMandatoryDefaultOff_neverEmails() {
    assertThat(NotificationEventType.emailEnabled(false, false, false)).isFalse();
    assertThat(NotificationEventType.emailEnabled(false, false, true)).isFalse();
  }

  @Test
  void mandatory_ignoresOptOut_andFollowsItsDefault() {
    // Mandatory + default on (e.g. account.contact-changed): always e-mails, opt-out is ignored.
    assertThat(NotificationEventType.emailEnabled(true, true, true)).isTrue();
    assertThat(NotificationEventType.emailEnabled(true, true, false)).isTrue();
    // Mandatory + default off (e.g. account.password-changed — identity already e-mails, DL-0008):
    // the module never e-mails, regardless of opt-out.
    assertThat(NotificationEventType.emailEnabled(true, false, false)).isFalse();
    assertThat(NotificationEventType.emailEnabled(true, false, true)).isFalse();
  }
}
