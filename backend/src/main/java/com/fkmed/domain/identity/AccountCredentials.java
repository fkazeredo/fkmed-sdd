package com.fkmed.domain.identity;

import java.util.UUID;

/**
 * Login-facing view of an account, exposed by {@link IdentityAccounts} to the infra security layer
 * (the {@code UserDetailsService} adapter and audit listeners). Carries the stored password hash
 * for the authentication provider to match; never leaves the server.
 *
 * @param accountId the account id.
 * @param email the login e-mail.
 * @param passwordHash the stored (delegating-encoder) password hash.
 * @param status the lifecycle state — only {@link AccountStatus#ACTIVE} may authenticate (BR6).
 * @param beneficiaryId the linked beneficiary (for the card claim and audit target).
 */
public record AccountCredentials(
    UUID accountId, String email, String passwordHash, AccountStatus status, UUID beneficiaryId) {}
