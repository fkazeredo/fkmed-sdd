package com.fkmed.infra.security;

import com.fkmed.domain.identity.AccountStatus;
import com.fkmed.domain.identity.IdentityAccounts;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Real account store behind the embedded Authorization Server's form login (SPEC-0002):
 * authenticates against {@code user_account} by e-mail. An {@link AccountStatus#EMAIL_NOT_VERIFIED}
 * account is returned {@code disabled}, so the provider raises {@code DisabledException} → the
 * "verify your e-mail" message with a resend affordance (BR6); a locked account (BR8) is returned
 * {@code accountLocked}, so the provider raises {@code LockedException} BEFORE the password check —
 * refusing login even with the correct password; an unknown e-mail yields the neutral not-found
 * (BR7, no enumeration). Replaces the retired dev-login seam (SPEC-0001 BR8).
 */
@Service
@RequiredArgsConstructor
public class IdentityUserDetailsService implements UserDetailsService {

  private final IdentityAccounts accounts;

  @Override
  public UserDetails loadUserByUsername(String username) {
    return accounts
        .findByEmail(username)
        .map(
            credentials ->
                User.withUsername(credentials.email())
                    .password(credentials.passwordHash())
                    .disabled(credentials.status() != AccountStatus.ACTIVE)
                    .accountLocked(credentials.locked())
                    .roles("BENEFICIARY")
                    .build())
        .orElseThrow(() -> new UsernameNotFoundException("no account for the given e-mail"));
  }
}
