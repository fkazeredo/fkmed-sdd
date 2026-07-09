package com.fkmed.infra.platform;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** DL-0032: transport layers keep enough headroom for SPEC-0015 reimbursement uploads. */
class UploadTransportConfigurationTest {

  @Test
  void springMultipartRequestLimit_allowsTheReimbursementBusinessTotal() throws Exception {
    String yaml =
        Files.readString(Path.of("src/main/resources/application.yaml"), StandardCharsets.UTF_8);

    assertThat(yaml).contains("max-file-size: 10MB");
    assertThat(yaml).contains("max-request-size: 25MB");
  }

  @Test
  void nginxBodyLimits_matchTheSpringRequestHeadroom() throws Exception {
    String frontendProxy =
        Files.readString(
            Path.of("..", "frontend", "nginx", "default.conf.template"), StandardCharsets.UTF_8);
    String tlsProxy =
        Files.readString(Path.of("..", "infra", "nginx", "tls-proxy.conf"), StandardCharsets.UTF_8);

    assertThat(frontendProxy).contains("client_max_body_size 25m;");
    assertThat(tlsProxy).contains("client_max_body_size 25m;");
  }
}
