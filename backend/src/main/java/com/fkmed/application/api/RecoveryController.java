package com.fkmed.application.api;

import com.fkmed.application.api.dto.PasswordRecoveryRequest;
import com.fkmed.application.api.dto.PasswordResetRequest;
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

/** Password-recovery endpoints (SPEC-0002 §API Contracts, BR10): neutral request and reset. */
@RestController
@RequestMapping("/api/auth/recovery")
@RequiredArgsConstructor
public class RecoveryController {

  private final IdentityService identityService;

  @PostMapping("/request")
  @ResponseStatus(HttpStatus.ACCEPTED)
  void request(@Valid @RequestBody PasswordRecoveryRequest request) {
    identityService.requestPasswordRecovery(request.email(), HttpRequestMetadata.current());
  }

  @PostMapping("/reset")
  void reset(@Valid @RequestBody PasswordResetRequest request) {
    identityService.resetPassword(
        request.token(), request.newPassword(), HttpRequestMetadata.current());
  }
}
