package com.fkmed.infra.identity;

import com.fkmed.domain.identity.CommonPasswordDenylist;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * Loads the curated common-passwords denylist (SPEC-0002 BR9) from the seeded resource {@code
 * classpath:/security/common-passwords.txt}; blank lines and {@code #} comments are ignored,
 * entries normalized to lowercase. Loaded once at startup into an immutable set (membership is O(1)
 * and hot on every registration).
 */
@Component
@Slf4j
public class ResourceCommonPasswordDenylist implements CommonPasswordDenylist {

  private static final String RESOURCE = "security/common-passwords.txt";

  private final Set<String> denied;

  public ResourceCommonPasswordDenylist() throws IOException {
    try (BufferedReader reader =
        new BufferedReader(
            new InputStreamReader(
                new ClassPathResource(RESOURCE).getInputStream(), StandardCharsets.UTF_8))) {
      this.denied =
          reader
              .lines()
              .map(String::trim)
              .filter(line -> !line.isEmpty() && !line.startsWith("#"))
              .map(line -> line.toLowerCase(Locale.ROOT))
              .collect(Collectors.toUnmodifiableSet());
    }
    log.info("loaded {} common-password denylist entries", denied.size());
  }

  @Override
  public boolean contains(String password) {
    return password != null && denied.contains(password.trim().toLowerCase(Locale.ROOT));
  }
}
