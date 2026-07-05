package com.fkmed.domain.plan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.time.Duration;
import org.junit.jupiter.api.Test;

/**
 * SPEC-0006 partial-update semantics and field validation (BR6/BR7 + §Validation Rules),
 * unit-tested on the {@link ContactInfo} value object with a fake UF registry — no persistence
 * needed.
 */
class ContactInfoTest {

  private static final UfValidator UF = uf -> uf.equals("RJ") || uf.equals("SP");

  private static ContactInfo base() {
    return ContactInfo.empty()
        .apply(
            new ContactUpdate(
                "maria@contato.com",
                "(21) 99999-1234",
                "(21) 2222-1010",
                "20040002",
                "Rua A",
                "10",
                "ap 1",
                "Centro",
                "Rio de Janeiro",
                "RJ"),
            UF);
  }

  @Test
  void partialUpdate_changesOnlySentFields_andKeepsTheRest() {
    ContactInfo updated = base().apply(onlyMobile("(11) 98888-7777"), UF);

    assertThat(updated.getMobile()).isEqualTo("(11) 98888-7777");
    assertThat(updated.getContactEmail()).isEqualTo("maria@contato.com");
    assertThat(updated.getCity()).isEqualTo("Rio de Janeiro");
    assertThat(updated.getUf()).isEqualTo("RJ");
  }

  @Test
  void emptyingMandatoryMobile_isRejected() {
    assertThatThrownBy(() -> base().apply(onlyMobile(""), UF))
        .isInstanceOf(MobileRequiredException.class);
  }

  @Test
  void emptyingMandatoryContactEmail_isRejected() {
    assertThatThrownBy(() -> base().apply(onlyEmail(""), UF))
        .isInstanceOf(ContactEmailRequiredException.class);
  }

  @Test
  void invalidContactEmail_isRejected() {
    assertThatThrownBy(() -> base().apply(onlyEmail("not-an-email"), UF))
        .isInstanceOf(ContactEmailInvalidException.class);
  }

  @Test
  void multiLabelDomainEmail_isAccepted() {
    // Semantics preserved by the linear pattern: a domain with several dot-separated labels stays
    // valid (regression guard for the ReDoS rewrite of the EMAIL pattern).
    ContactInfo updated = base().apply(onlyEmail("maria@correio.fkmed.com.br"), UF);
    assertThat(updated.getContactEmail()).isEqualTo("maria@correio.fkmed.com.br");
  }

  @Test
  void emailWithoutDotInDomain_isRejected() {
    // Semantics preserved: an at without a dotted domain is still invalid.
    assertThatThrownBy(() -> base().apply(onlyEmail("maria@localhost"), UF))
        .isInstanceOf(ContactEmailInvalidException.class);
  }

  @Test
  void adversarialEmail_isRejectedWithoutCatastrophicBacktracking() {
    // Regression for the CodeQL HIGH "polynomial regular expression used on uncontrolled data": the
    // previous ambiguous pattern `[^@\s]+\.[^@\s]+` backtracked in O(n^2) on inputs like
    // `a@` + `.`×n + `@`. The linear pattern (plus the RFC length bound) must reject this crafted
    // input in bounded time instead of hanging. This test times out (fails) on the old code.
    String adversarial = "a@" + ".".repeat(40_000) + "@";
    assertTimeoutPreemptively(
        Duration.ofSeconds(2),
        () ->
            assertThatThrownBy(() -> base().apply(onlyEmail(adversarial), UF))
                .isInstanceOf(ContactEmailInvalidException.class));
  }

  @Test
  void invalidMobileFormat_isRejected() {
    assertThatThrownBy(() -> base().apply(onlyMobile("21999991234"), UF))
        .isInstanceOf(MobileInvalidException.class);
  }

  @Test
  void invalidLandlineFormat_isRejected() {
    assertThatThrownBy(() -> base().apply(only(2, "2122221010"), UF))
        .isInstanceOf(LandlineInvalidException.class);
  }

  @Test
  void optionalLandline_clearedWhenSentEmpty() {
    ContactInfo updated = base().apply(only(2, ""), UF);
    assertThat(updated.getLandline()).isNull();
  }

  @Test
  void invalidCep_isRejected() {
    assertThatThrownBy(() -> base().apply(only(3, "123"), UF))
        .isInstanceOf(CepInvalidException.class);
  }

  @Test
  void unknownUf_isRejected() {
    assertThatThrownBy(() -> base().apply(only(9, "ZZ"), UF))
        .isInstanceOf(UfInvalidException.class);
  }

  @Test
  void uf_isUpperCasedBeforeValidation() {
    ContactInfo updated = base().apply(only(9, "sp"), UF);
    assertThat(updated.getUf()).isEqualTo("SP");
  }

  private static ContactUpdate onlyEmail(String email) {
    return only(0, email);
  }

  private static ContactUpdate onlyMobile(String mobile) {
    return only(1, mobile);
  }

  /** A partial update with only field {@code index} set (0=email … 9=uf), the rest {@code null}. */
  private static ContactUpdate only(int index, String value) {
    String[] f = new String[10];
    f[index] = value;
    return new ContactUpdate(f[0], f[1], f[2], f[3], f[4], f[5], f[6], f[7], f[8], f[9]);
  }
}
