package com.fkmed.application.api;

import com.fkmed.application.api.dto.GenerateTokenRequest;
import com.fkmed.domain.guides.TokenService;
import com.fkmed.domain.guides.TokenView;
import com.fkmed.infra.security.UserContextProvider;
import com.fkmed.infra.web.HttpRequestMetadata;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Attendance-token endpoints (SPEC-0012 BR9-BR12): generate (invalidating any previous active
 * token) and read the current one. Family scope, single-validity and the BR12 dependent-authorship
 * audit all live in {@code domain.guides.TokenService}; the caller's beneficiary card and acting
 * account are resolved from the JWT, never client-supplied.
 */
@RestController
@RequestMapping("/api/tokens")
@RequiredArgsConstructor
public class TokenController {

  private final TokenService tokens;
  private final UserContextProvider userContext;

  /** Generates a fresh token, invalidating any previous active one for the beneficiary (BR9). */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  TokenView generate(@Valid @RequestBody GenerateTokenRequest request) {
    return tokens.generate(
        callerCard(), authorEmail(), request.beneficiaryId(), HttpRequestMetadata.current());
  }

  /** The beneficiary's current valid token, if any (BR9/BR10). */
  @GetMapping("/current")
  TokenView current(@RequestParam UUID beneficiaryId) {
    return tokens.current(callerCard(), beneficiaryId);
  }

  private String callerCard() {
    return userContext.current().beneficiaryCard().orElse(null);
  }

  private String authorEmail() {
    return userContext.current().username();
  }
}
