package com.fkmed.domain.identity;

import java.util.Locale;

/** Login-e-mail normalization shared across the identity module (trim + lowercase). */
final class Emails {

  private Emails() {}

  static String normalize(String email) {
    return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
  }
}
