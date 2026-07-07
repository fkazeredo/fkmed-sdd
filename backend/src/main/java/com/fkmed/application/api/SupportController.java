package com.fkmed.application.api;

import com.fkmed.application.api.dto.LibrasServiceRequest;
import com.fkmed.domain.support.AntifraudView;
import com.fkmed.domain.support.FaqEntryView;
import com.fkmed.domain.support.LibrasRequestResult;
import com.fkmed.domain.support.SupportChannelView;
import com.fkmed.domain.support.SupportService;
import com.fkmed.infra.security.UserContextProvider;
import com.fkmed.infra.web.HttpRequestMetadata;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Support endpoints (SPEC-0014 — Canais de Atendimento e FAQ): channel cards, the antifraud
 * section, FAQ search and Libras service-request registration. Every route only requires
 * authentication (content-serving); the Libras write additionally scope-checks {@code
 * beneficiaryId} against the caller's family in {@code domain.support.SupportService}.
 */
@RestController
@RequestMapping("/api/support")
@RequiredArgsConstructor
public class SupportController {

  private final SupportService support;
  private final UserContextProvider userContext;

  /** BR1: the channel cards. */
  @GetMapping("/channels")
  List<SupportChannelView> channels() {
    return support.channels();
  }

  /** BR3: the antifraud section content. */
  @GetMapping("/antifraud")
  AntifraudView antifraud() {
    return support.antifraud();
  }

  /** BR5: FAQ entries filtered by an optional category and/or a real-time search term. */
  @GetMapping("/faq")
  List<FaqEntryView> faq(
      @RequestParam(required = false) String q, @RequestParam(required = false) String category) {
    return support.faq(q, category);
  }

  /** BR4: registers a Libras service request for a beneficiary within the caller's scope. */
  @PostMapping("/libras-requests")
  @ResponseStatus(HttpStatus.CREATED)
  LibrasRequestResult registerLibrasRequest(@Valid @RequestBody LibrasServiceRequest request) {
    return support.requestLibras(
        callerCard(), authorEmail(), request.beneficiaryId(), HttpRequestMetadata.current());
  }

  private String callerCard() {
    return userContext.current().beneficiaryCard().orElse(null);
  }

  private String authorEmail() {
    return userContext.current().username();
  }
}
