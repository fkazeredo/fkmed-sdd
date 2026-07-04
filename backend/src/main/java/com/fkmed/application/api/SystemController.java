package com.fkmed.application.api;

import com.fkmed.application.api.dto.SystemHealthResponse;
import com.fkmed.application.api.dto.SystemVersionResponse;
import com.fkmed.infra.health.DatabaseStatusProbe;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.info.GitProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Public system endpoints (SPEC-0001 BR1/BR2): health and build version. */
@RestController
@RequestMapping("/api/system")
@RequiredArgsConstructor
public class SystemController {

  private final DatabaseStatusProbe databaseStatusProbe;
  private final BuildProperties buildProperties;
  private final ObjectProvider<GitProperties> gitProperties;

  @GetMapping("/health")
  SystemHealthResponse health() {
    return SystemHealthResponse.of(databaseStatusProbe.isUp());
  }

  @GetMapping("/version")
  SystemVersionResponse version() {
    GitProperties git = gitProperties.getIfAvailable();
    return new SystemVersionResponse(
        buildProperties.getVersion(), git != null ? git.getShortCommitId() : "unknown");
  }
}
