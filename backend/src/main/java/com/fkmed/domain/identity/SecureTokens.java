package com.fkmed.domain.identity;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * High-entropy token generation and hashing for verification links (SPEC-0002 BR5). The raw token
 * (256 random bits, URL-safe) goes to the user; only its SHA-256 hex is persisted, so a database
 * read never yields a usable link.
 */
public final class SecureTokens {

  private static final SecureRandom RANDOM = new SecureRandom();

  private SecureTokens() {}

  /** A fresh URL-safe 256-bit token. */
  public static String newRawToken() {
    byte[] bytes = new byte[32];
    RANDOM.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  /** Lowercase hex SHA-256 of {@code value} (64 chars). */
  public static String sha256Hex(String value) {
    try {
      byte[] hash =
          MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
      StringBuilder hex = new StringBuilder(hash.length * 2);
      for (byte b : hash) {
        hex.append(Character.forDigit((b >> 4) & 0xF, 16));
        hex.append(Character.forDigit(b & 0xF, 16));
      }
      return hex.toString();
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 unavailable", e);
    }
  }
}
