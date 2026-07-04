package com.fkmed.application.api;

import com.fkmed.application.api.dto.ChangePasswordRequest;
import com.fkmed.domain.identity.IdentityService;
import com.fkmed.infra.security.UserContextProvider;
import com.fkmed.infra.web.HttpRequestMetadata;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Authenticated password change (SPEC-0002 §API Contracts, BR11): the caller's identity comes from
 * the resource-server JWT via {@link UserContextProvider} (never a client-supplied e-mail).
 */
@RestController
@RequestMapping("/api/auth/password")
@RequiredArgsConstructor
public class PasswordController {

  private final IdentityService identityService;
  private final UserContextProvider userContext;

  @PutMapping
  void change(@Valid @RequestBody ChangePasswordRequest request) {
    identityService.changePassword(
        userContext.current().username(),
        request.currentPassword(),
        request.newPassword(),
        HttpRequestMetadata.current());
  }
}
