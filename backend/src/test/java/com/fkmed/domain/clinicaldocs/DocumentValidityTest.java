package com.fkmed.domain.clinicaldocs;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/**
 * SPEC-0011 BR4/DL-0019: per-type validity defaults and the BR5 read-time expiry boundary (day
 * 30/90 — the document stays valid through the stamped date, inclusive).
 */
class DocumentValidityTest {

  private static final LocalDate ISSUED = LocalDate.of(2026, 1, 1);

  @Test
  void validUntilFor_prescription_is30DaysFromIssue() {
    assertThat(DocumentValidity.validUntilFor(ClinicalDocumentType.PRESCRIPTION, ISSUED))
        .isEqualTo(LocalDate.of(2026, 1, 31));
  }

  @Test
  void validUntilFor_examOrder_is90DaysFromIssue() {
    assertThat(DocumentValidity.validUntilFor(ClinicalDocumentType.EXAM_ORDER, ISSUED))
        .isEqualTo(ISSUED.plusDays(90));
  }

  @Test
  void validUntilFor_referral_is90DaysFromIssue() {
    assertThat(DocumentValidity.validUntilFor(ClinicalDocumentType.REFERRAL, ISSUED))
        .isEqualTo(ISSUED.plusDays(90));
  }

  @Test
  void validUntilFor_sickNote_isNull() {
    assertThat(DocumentValidity.validUntilFor(ClinicalDocumentType.SICK_NOTE, ISSUED)).isNull();
  }

  @Test
  void isExpired_onTheValidUntilDay_isFalse_boundaryInclusive() {
    LocalDate validUntil = ISSUED.plusDays(30);
    assertThat(DocumentValidity.isExpired(validUntil, validUntil)).isFalse();
  }

  @Test
  void isExpired_theDayAfterValidUntil_isTrue() {
    LocalDate validUntil = ISSUED.plusDays(30);
    assertThat(DocumentValidity.isExpired(validUntil, validUntil.plusDays(1))).isTrue();
  }

  @Test
  void isExpired_dayBeforeValidUntil_isFalse() {
    LocalDate validUntil = ISSUED.plusDays(90);
    assertThat(DocumentValidity.isExpired(validUntil, validUntil.minusDays(1))).isFalse();
  }

  @Test
  void isExpired_noValidity_neverExpires() {
    assertThat(DocumentValidity.isExpired(null, LocalDate.of(2099, 12, 31))).isFalse();
  }
}
