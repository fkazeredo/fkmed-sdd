package com.fkmed.domain.identity;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * The registration password policy (SPEC-0002 BR9 / RN-AUTH-01), enforced server-side and mirrored
 * client-side (BR16). A password MUST have at least 8 characters, at least one letter and one
 * digit, MUST NOT equal the login e-mail, and MUST NOT appear in the common-passwords denylist.
 * Pure domain logic (no persistence) so it is exhaustively unit- and mutation-tested.
 */
@Component
@RequiredArgsConstructor
public class PasswordPolicy {

  private static final int MIN_LENGTH = 8;

  private final CommonPasswordDenylist denylist;

  /**
   * Validates the password for the given login e-mail.
   *
   * @throws PasswordPolicyViolationException when any rule fails (the reason is deliberately not
   *     disclosed — the message is generic).
   */
  public void validate(String email, String password) {
    if (password == null
        || password.length() < MIN_LENGTH
        || !hasLetter(password)
        || !hasDigit(password)
        || equalsEmail(email, password)
        || denylist.contains(password)) {
      throw new PasswordPolicyViolationException();
    }
  }

  private static boolean hasLetter(String password) {
    return password.chars().anyMatch(Character::isLetter);
  }

  private static boolean hasDigit(String password) {
    return password.chars().anyMatch(Character::isDigit);
  }

  private static boolean equalsEmail(String email, String password) {
    return email != null && email.trim().equalsIgnoreCase(password.trim());
  }
}
