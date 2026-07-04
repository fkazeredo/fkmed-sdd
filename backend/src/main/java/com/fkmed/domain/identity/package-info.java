/**
 * The identity module (SPEC-0002): self-service account creation (first access), e-mail
 * verification and the account lifecycle behind real login.
 *
 * <p>Owns {@code user_account}, {@code email_verification_token} and {@code term_acceptance}
 * (Flyway V3). Depends on the {@code domain.error} kernel, on the {@code domain.audit} recorder,
 * and on the {@code domain.plan} public facade ({@link com.fkmed.domain.plan.Beneficiaries}) to
 * match the identity triple and read the beneficiary card — never on another module's internals. It
 * publishes {@link com.fkmed.domain.identity.AccountCreated}; e-mail delivery is an infra listener
 * over the {@code MailSender} port (ADR-0004). Lockout/recovery/password-change are SLICE 1.2+.
 * Module map: ADR-0001.
 */
@org.springframework.modulith.ApplicationModule(displayName = "identity")
package com.fkmed.domain.identity;
