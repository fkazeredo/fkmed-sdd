package com.fkmed.domain.identity;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

/** SPEC-0002 BR9 / RN-AUTH-01: the registration and change password policy, rule by rule. */
class PasswordPolicyTest {

  private static final Set<String> DENYLIST = Set.of("senha123", "password1");

  /** Deterministic encoder so the differ-from-current check is exercised without BCrypt cost. */
  private static final PasswordEncoder ENCODER =
      new PasswordEncoder() {
        @Override
        public String encode(CharSequence raw) {
          return "{enc}" + raw;
        }

        @Override
        public boolean matches(CharSequence raw, String encoded) {
          return encoded.equals("{enc}" + raw);
        }
      };

  private final PasswordPolicy policy =
      new PasswordPolicy(password -> DENYLIST.contains(password.toLowerCase()));

  @Test
  void accepts_aCompliantPassword() {
    assertThatCode(() -> policy.validate("user@fkmed.local", "Abcd1234"))
        .doesNotThrowAnyException();
  }

  @Test
  void accepts_exactlyEightCharsWithLetterAndDigit() {
    assertThatCode(() -> policy.validate("user@fkmed.local", "abcdefg1"))
        .doesNotThrowAnyException();
  }

  @Test
  void rejects_null() {
    assertThatExceptionOfType(PasswordPolicyViolationException.class)
        .isThrownBy(() -> policy.validate("user@fkmed.local", null));
  }

  @Test
  void rejects_shorterThanEight() {
    assertThatExceptionOfType(PasswordPolicyViolationException.class)
        .isThrownBy(() -> policy.validate("user@fkmed.local", "abcde12"));
  }

  @Test
  void rejects_withoutADigit() {
    assertThatExceptionOfType(PasswordPolicyViolationException.class)
        .isThrownBy(() -> policy.validate("user@fkmed.local", "abcdefgh"));
  }

  @Test
  void rejects_withoutALetter() {
    assertThatExceptionOfType(PasswordPolicyViolationException.class)
        .isThrownBy(() -> policy.validate("user@fkmed.local", "12345678"));
  }

  @Test
  void rejects_equalToTheLoginEmail_caseInsensitive() {
    assertThatExceptionOfType(PasswordPolicyViolationException.class)
        .isThrownBy(() -> policy.validate("abcdefg1", "ABCDEFG1"));
  }

  @Test
  void rejects_aCommonPassword() {
    assertThatExceptionOfType(PasswordPolicyViolationException.class)
        .isThrownBy(() -> policy.validate("user@fkmed.local", "senha123"));
  }

  // ---- BR9 last clause: an authenticated change must differ from the current password ----

  @Test
  void validateChange_acceptsACompliantNewPasswordDifferentFromTheCurrent() {
    String currentHash = ENCODER.encode("OldPass123");
    assertThatCode(
            () -> policy.validateChange("user@fkmed.local", "NewPass123", currentHash, ENCODER))
        .doesNotThrowAnyException();
  }

  @Test
  void validateChange_rejectsANewPasswordEqualToTheCurrentOne() {
    String currentHash = ENCODER.encode("SamePass123");
    assertThatExceptionOfType(PasswordPolicyViolationException.class)
        .isThrownBy(
            () -> policy.validateChange("user@fkmed.local", "SamePass123", currentHash, ENCODER));
  }

  @Test
  void validateChange_stillEnforcesTheBasePolicy() {
    String currentHash = ENCODER.encode("OldPass123");
    assertThatExceptionOfType(PasswordPolicyViolationException.class)
        .isThrownBy(() -> policy.validateChange("user@fkmed.local", "short", currentHash, ENCODER));
  }
}
