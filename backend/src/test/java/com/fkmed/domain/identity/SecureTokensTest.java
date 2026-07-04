package com.fkmed.domain.identity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Base64;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

/**
 * SPEC-0002 BR5: token generation and hashing behind e-mail verification links. Direct unit
 * coverage (PIT-confirmed gap: nothing indirectly caught a removed {@code SecureRandom::nextBytes}
 * call or math/return mutants in {@code sha256Hex}).
 */
class SecureTokensTest {

  @Test
  void sha256Hex_matchesTheKnownTestVectorForAnEmptyString() {
    // Well-known SHA-256 test vector.
    assertThat(SecureTokens.sha256Hex(""))
        .isEqualTo("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
  }

  @Test
  void sha256Hex_matchesTheKnownTestVectorForAbc() {
    // Well-known SHA-256 test vector (NIST FIPS 180 example).
    assertThat(SecureTokens.sha256Hex("abc"))
        .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
  }

  @Test
  void sha256Hex_isDeterministic_sameInputSameHash() {
    assertThat(SecureTokens.sha256Hex("raw-token")).isEqualTo(SecureTokens.sha256Hex("raw-token"));
  }

  @Test
  void sha256Hex_isSixtyFourLowercaseHexChars() {
    String hash = SecureTokens.sha256Hex("anything");
    assertThat(hash).hasSize(64).matches("^[0-9a-f]{64}$");
  }

  @Test
  void sha256Hex_differentInputs_differentHashes() {
    assertThat(SecureTokens.sha256Hex("token-a")).isNotEqualTo(SecureTokens.sha256Hex("token-b"));
  }

  @Test
  void newRawToken_isUrlSafeBase64_ofTwoHundredFiftySixBits() {
    String token = SecureTokens.newRawToken();
    // URL-safe alphabet only (no '+', '/'; padding stripped) — safe to embed in a query string.
    assertThat(token).matches("^[A-Za-z0-9_-]+$");
    assertThat(Base64.getUrlDecoder().decode(token)).hasSize(32); // 256 bits
  }

  @Test
  void newRawToken_producesAdequatelyRandomAndUniqueOutputAcrossCalls() {
    // Not a full entropy proof — just guards against a broken/removed SecureRandom call
    // (e.g. a mutant returning a constant or all-zero token every time).
    Set<String> tokens = new HashSet<>();
    IntStream.range(0, 200).forEach(i -> tokens.add(SecureTokens.newRawToken()));
    assertThat(tokens).hasSize(200);
  }
}
