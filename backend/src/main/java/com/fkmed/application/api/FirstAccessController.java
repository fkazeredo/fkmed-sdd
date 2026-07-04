package com.fkmed.application.api;

import com.fkmed.application.api.dto.CompleteFirstAccessRequest;
import com.fkmed.application.api.dto.VerifyFirstAccessRequest;
import com.fkmed.application.api.dto.VerifyFirstAccessResponse;
import com.fkmed.domain.identity.IdentityService;
import com.fkmed.infra.web.HttpRequestMetadata;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * First-access endpoints (SPEC-0002 §API Contracts): identity verification and account creation.
 */
@RestController
@RequestMapping("/api/auth/first-access")
@RequiredArgsConstructor
public class FirstAccessController {

  private final IdentityService identityService;

  @PostMapping("/verify")
  VerifyFirstAccessResponse verify(@Valid @RequestBody VerifyFirstAccessRequest request) {
    return new VerifyFirstAccessResponse(
        identityService.verifyFirstAccess(
            request.cpf(), request.cardNumber(), request.birthDate()));
  }

  @PostMapping("/complete")
  @ResponseStatus(HttpStatus.CREATED)
  void complete(@Valid @RequestBody CompleteFirstAccessRequest request) {
    identityService.completeFirstAccess(
        request.registrationToken(),
        request.email(),
        request.password(),
        HttpRequestMetadata.current());
  }
}
