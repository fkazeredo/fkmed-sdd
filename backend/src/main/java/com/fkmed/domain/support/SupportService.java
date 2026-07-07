package com.fkmed.domain.support;

import com.fkmed.domain.audit.AuditContext;
import com.fkmed.domain.audit.AuditEntry;
import com.fkmed.domain.audit.AuditEventTypes;
import com.fkmed.domain.audit.AuditRecorder;
import com.fkmed.domain.identity.AccountCredentials;
import com.fkmed.domain.identity.IdentityAccounts;
import com.fkmed.domain.plan.BeneficiaryAccess;
import io.micrometer.core.instrument.MeterRegistry;
import java.text.Normalizer;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service and public facade of the support module (SPEC-0014): channel cards (BR1),
 * antifraud content (BR3), FAQ search (BR5/BR6) and Libras service-request registration (BR4).
 * Every read is content-serving, no scope check; the Libras write is family-scope-checked through
 * {@link BeneficiaryAccess} (SPEC-0003 BR3), mirroring {@code domain.guides.TokenService}.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SupportService {

  private final SupportChannelRepository channels;
  private final FaqEntryRepository faqEntries;
  private final SupportAntifraudRepository antifraudContent;
  private final LibrasRequestRepository librasRequests;
  private final BeneficiaryAccess beneficiaryAccess;
  private final IdentityAccounts identityAccounts;
  private final AuditRecorder auditRecorder;
  private final MeterRegistry metrics;
  private final Clock clock;

  /** BR1: the channel cards in display order. */
  public List<SupportChannelView> channels() {
    return channels.findAllByOrderByDisplayOrderAsc().stream()
        .map(
            channel ->
                new SupportChannelView(
                    channel.getType(),
                    channel.getLabel(),
                    channel.getSublabel(),
                    channel.getValue(),
                    channel.getHours(),
                    channel.getDisplayOrder()))
        .toList();
  }

  /**
   * BR3: the antifraud section content.
   *
   * @throws IllegalStateException when the single-row seed is missing — an internal-contract
   *     violation (the V25 seed always inserts exactly one row), never client-facing.
   */
  public AntifraudView antifraud() {
    SupportAntifraud content =
        antifraudContent.findAll().stream()
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("support_antifraud seed is missing"));
    return new AntifraudView(content.getTitle(), content.getMessage());
  }

  /**
   * BR5: FAQ entries matching {@code category} (when given) and {@code rawQuery} (real-time,
   * case/accent-insensitive over title AND content — BR5). Counts a zero-result search as a
   * content-gap signal (§Observability), but only when a query was actually typed — an empty query
   * legitimately lists everything.
   */
  public List<FaqEntryView> faq(String rawQuery, String category) {
    String normalizedQuery = normalize(rawQuery);
    List<FaqEntry> matches =
        faqEntries.findByActiveTrueOrderByDisplayOrderAsc().stream()
            .filter(
                entry ->
                    category == null
                        || category.isBlank()
                        || category.equalsIgnoreCase(entry.getCategory()))
            .filter(
                entry ->
                    normalizedQuery.isEmpty()
                        || normalize(entry.getQuestion()).contains(normalizedQuery)
                        || normalize(entry.getAnswer()).contains(normalizedQuery))
            .toList();
    if (!normalizedQuery.isEmpty() && matches.isEmpty()) {
      metrics.counter("support.faq.zero-results").increment();
    }
    return matches.stream()
        .map(
            entry ->
                new FaqEntryView(
                    entry.getId(),
                    entry.getCategory(),
                    entry.getQuestion(),
                    entry.getAnswer(),
                    entry.getDisplayOrder()))
        .toList();
  }

  /**
   * BR4: registers a Libras service request for {@code beneficiaryId} within the caller's family
   * scope, audits it (§Observability) and reports whether the team starts "in instants" or the
   * caller is offered the next operating period.
   *
   * @throws com.fkmed.domain.plan.BeneficiaryNotAccessibleException when the beneficiary is out of
   *     the caller's scope.
   */
  @Transactional
  public LibrasRequestResult requestLibras(
      String callerCard, String authorEmail, UUID beneficiaryId, AuditContext auditContext) {
    beneficiaryAccess.requireAccessible(callerCard, beneficiaryId);

    Instant now = clock.instant();
    LibrasRequest request = LibrasRequest.register(beneficiaryId, now);
    librasRequests.save(request);

    boolean withinHours = LibrasHours.isWithin(now);
    metrics
        .counter("support.libras.requested", "withinHours", String.valueOf(withinHours))
        .increment();
    auditRecorder.record(
        new AuditEntry(
            AuditEventTypes.SUPPORT_LIBRAS_REQUESTED,
            authorAccountIdFor(authorEmail),
            beneficiaryId,
            Map.of(),
            auditContext));

    return withinHours ? LibrasRequestResult.registered() : LibrasRequestResult.nextPeriod();
  }

  private UUID authorAccountIdFor(String email) {
    return identityAccounts.findByEmail(email).map(AccountCredentials::accountId).orElse(null);
  }

  /** Case/accent-insensitive normalization for FAQ search matching (BR5). */
  private static String normalize(String value) {
    if (value == null) {
      return "";
    }
    String decomposed = Normalizer.normalize(value, Normalizer.Form.NFD);
    return decomposed.replaceAll("\\p{M}", "").toLowerCase().strip();
  }
}
