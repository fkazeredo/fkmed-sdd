package com.fkmed.infra.security;

import com.fkmed.domain.audit.AuditEntry;
import com.fkmed.domain.audit.AuditEventTypes;
import com.fkmed.domain.audit.AuditRecorder;
import com.fkmed.domain.audit.Masking;
import com.fkmed.domain.identity.IdentityAccounts;
import com.fkmed.infra.web.HttpRequestMetadata;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;

/** Records logout to the audit trail (SPEC-0002 BR14); registered on the form-login chain. */
@Component
@RequiredArgsConstructor
public class LogoutAuditRecorder implements LogoutHandler {

  private final AuditRecorder auditRecorder;
  private final IdentityAccounts accounts;

  @Override
  public void logout(
      HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
    if (authentication == null) {
      return;
    }
    String email = authentication.getName();
    accounts
        .findByEmail(email)
        .ifPresent(
            credentials ->
                auditRecorder.record(
                    new AuditEntry(
                        AuditEventTypes.LOGOUT,
                        credentials.accountId(),
                        credentials.beneficiaryId(),
                        Map.of("email", Masking.email(email)),
                        HttpRequestMetadata.current())));
  }
}
