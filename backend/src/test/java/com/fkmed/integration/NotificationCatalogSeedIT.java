package com.fkmed.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * SPEC-0004 BR5 / DL-0008: V10 seeds the notification event-type registry with the catalog
 * referenced across specs and the exact channel defaults — account/security types are mandatory;
 * the two types the identity e-mail seam already sends keep {@code email_default=false} so this
 * module never double-sends; business types e-mail by default and are opt-outable.
 */
class NotificationCatalogSeedIT extends AbstractIntegrationTest {

  @Autowired private JdbcTemplate jdbc;

  @Test
  void v10_seedsTheEventTypeCatalogWithTheExpectedChannelDefaults() {
    // V10 seeded 6 types; V17 (SPEC-0009) added appointment cancel/reschedule; V20 added
    // telemedicine turn/closure and clinical-document; V24 added finance invoice; V27 adds the
    // remaining reimbursement lifecycle and preview events.
    assertThat(count()).isEqualTo(20);

    assertType("account.password-changed", false, true);
    assertType("account.locked", false, true);
    assertType("account.contact-changed", true, true);
    assertType("reimbursement.paid", true, false);
    assertType("guide.status-changed", true, false);
    assertType("appointment.confirmed", true, false);
    assertType("appointment.cancelled", true, false);
    assertType("appointment.rescheduled", true, false);
    assertType("tele.turn-reached", true, false);
    assertType("tele.session-closed", true, false);
    assertType("clinical-document.issued", true, false);
    assertType("finance.invoice-issued", true, false);
    assertType("reimbursement.submitted", true, false);
    assertType("reimbursement.pendency-opened", true, false);
    assertType("reimbursement.pendency-resolved", true, false);
    assertType("reimbursement.approved", true, false);
    assertType("reimbursement.denied", true, false);
    assertType("reimbursement.payment-failed", true, false);
    assertType("reimbursement.cancelled", true, false);
    assertType("preview.concluded", true, false);
  }

  private void assertType(String code, boolean emailDefault, boolean mandatory) {
    Map<String, Object> row =
        jdbc.queryForMap("select * from notification_event_type where code = ?", code);
    assertThat(row.get("email_default")).as("email_default of %s", code).isEqualTo(emailDefault);
    assertThat(row.get("mandatory")).as("mandatory of %s", code).isEqualTo(mandatory);
    assertThat((String) row.get("description")).as("description of %s", code).isNotBlank();
  }

  private long count() {
    return jdbc.queryForObject("select count(*) from notification_event_type", Long.class);
  }
}
