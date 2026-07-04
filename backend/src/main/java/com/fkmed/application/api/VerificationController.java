package com.fkmed.application.api;

import com.fkmed.application.api.dto.ConfirmVerificationRequest;
import com.fkmed.application.api.dto.ResendVerificationRequest;
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

/** E-mail verification endpoints (SPEC-0002 §API Contracts): confirm and resend the link. */
@RestController
@RequestMapping("/api/auth/verification")
@RequiredArgsConstructor
public class VerificationController {

  private final IdentityService identityService;

  @PostMapping("/confirm")
  void confirm(@Valid @RequestBody ConfirmVerificationRequest request) {
    identityService.confirmVerification(request.token(), HttpRequestMetadata.current());
  }

  @PostMapping("/resend")
  @ResponseStatus(HttpStatus.ACCEPTED)
  void resend(@Valid @RequestBody ResendVerificationRequest request) {
    identityService.resendVerification(request.email());
  }
}
