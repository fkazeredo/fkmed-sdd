package com.fkmed.domain.identity;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Issues and verifies the short-lived, stateless {@code registrationToken} that bridges {@code
 * first-access/verify} and {@code first-access/complete} (DL-0001). The token is {@code
 * base64url(beneficiaryId|expiryEpoch).base64url(HMAC-SHA256)} — no table, no server state. Any
 * tampering or expiry yields the single generic {@link RegistrationNotFoundException} (BR1), so a
 * forged token is indistinguishable from a failed identity match.
 */
public final class RegistrationTokenService {

  private static final String HMAC_ALGORITHM = "HmacSHA256";
  private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
  private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

  private final byte[] secret;
  private final Clock clock;
  private final Duration ttl;

  public RegistrationTokenService(byte[] secret, Clock clock, Duration ttl) {
    this.secret = secret.clone();
    this.clock = clock;
    this.ttl = ttl;
  }

  /** Issues a token proving {@code beneficiaryId} passed the identity triple, valid for the TTL. */
  public String issue(UUID beneficiaryId) {
    long expiry = clock.instant().plus(ttl).getEpochSecond();
    byte[] payload = (beneficiaryId + "|" + expiry).getBytes(StandardCharsets.UTF_8);
    return ENCODER.encodeToString(payload) + "." + ENCODER.encodeToString(sign(payload));
  }

  /**
   * Verifies a token and returns the beneficiary id it certifies.
   *
   * @throws RegistrationNotFoundException when the token is malformed, tampered or expired.
   */
  public UUID verify(String token) {
    try {
      int dot = token.indexOf('.');
      if (dot <= 0) {
        throw new RegistrationNotFoundException();
      }
      byte[] payload = DECODER.decode(token.substring(0, dot));
      byte[] presentedSignature = DECODER.decode(token.substring(dot + 1));
      if (!MessageDigest.isEqual(sign(payload), presentedSignature)) {
        throw new RegistrationNotFoundException();
      }
      String[] parts = new String(payload, StandardCharsets.UTF_8).split("\\|", 2);
      if (parts.length != 2 || clock.instant().getEpochSecond() >= Long.parseLong(parts[1])) {
        throw new RegistrationNotFoundException();
      }
      return UUID.fromString(parts[0]);
    } catch (RegistrationNotFoundException e) {
      throw e;
    } catch (RuntimeException e) {
      throw new RegistrationNotFoundException();
    }
  }

  private byte[] sign(byte[] payload) {
    try {
      Mac mac = Mac.getInstance(HMAC_ALGORITHM);
      mac.init(new SecretKeySpec(secret, HMAC_ALGORITHM));
      return mac.doFinal(payload);
    } catch (NoSuchAlgorithmException | InvalidKeyException e) {
      throw new IllegalStateException("HMAC-SHA256 unavailable", e);
    }
  }
}
