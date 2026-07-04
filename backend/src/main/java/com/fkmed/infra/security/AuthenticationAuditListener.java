package com.fkmed.infra.security;

import com.fkmed.domain.audit.AuditEntry;
import com.fkmed.domain.audit.AuditEventTypes;
import com.fkmed.domain.audit.AuditRecorder;
import com.fkmed.domain.audit.Masking;
import com.fkmed.domain.identity.IdentityAccounts;
import com.fkmed.infra.web.HttpRequestMetadata;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

/**
 * Records form-login outcomes to the audit trail (SPEC-0002 BR14; SPEC-0003 BR6). Scoped to {@link
 * UsernamePasswordAuthenticationToken} so per-request resource-server (JWT) authentications are not
 * audited. Credentials are never recorded — only a masked e-mail hint (BR8).
 */
@Component
@RequiredArgsConstructor
public class AuthenticationAuditListener {

  private final AuditRecorder auditRecorder;
  private final IdentityAccounts accounts;

  @EventListener
  void onSuccess(AuthenticationSuccessEvent event) {
    if (!(event.getAuthentication() instanceof UsernamePasswordAuthenticationToken)) {
      return;
    }
    String email = event.getAuthentication().getName();
    accounts
        .findByEmail(email)
        .ifPresentOrElse(
            credentials ->
                auditRecorder.record(
                    new AuditEntry(
                        AuditEventTypes.LOGIN_SUCCESS,
                        credentials.accountId(),
                        credentials.beneficiaryId(),
                        Map.of("email", Masking.email(email)),
                        HttpRequestMetadata.current())),
            () ->
                auditRecorder.record(
                    new AuditEntry(
                        AuditEventTypes.LOGIN_SUCCESS,
                        null,
                        null,
                        Map.of("email", Masking.email(email)),
                        HttpRequestMetadata.current())));
  }

  @EventListener
  void onFailure(AbstractAuthenticationFailureEvent event) {
    if (!(event.getAuthentication() instanceof UsernamePasswordAuthenticationToken)) {
      return;
    }
    auditRecorder.record(
        new AuditEntry(
            AuditEventTypes.LOGIN_FAILURE,
            null,
            null,
            Map.of("email", Masking.email(String.valueOf(event.getAuthentication().getName()))),
            HttpRequestMetadata.current()));
  }
}
