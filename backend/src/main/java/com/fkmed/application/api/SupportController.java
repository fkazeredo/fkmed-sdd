package com.fkmed.application.api;

import com.fkmed.application.api.dto.LibrasRequestInput;
import com.fkmed.domain.support.AntifraudContent;
import com.fkmed.domain.support.AntifraudView;
import com.fkmed.domain.support.FaqQuestionView;
import com.fkmed.domain.support.FaqSearch;
import com.fkmed.domain.support.LibrasRequestResponse;
import com.fkmed.domain.support.LibrasRequests;
import com.fkmed.domain.support.SupportChannelView;
import com.fkmed.domain.support.SupportChannels;
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
 * Canais de Atendimento endpoints (SPEC-0014): channel cards (BR1/BR2), antifraud copy (BR3), FAQ
 * (BR5/BR6) and the Central de Libras service request (BR4). Content-serving module — no new
 * business error codes, standard auth/scope only.
 */
@RestController
@RequestMapping("/api/support")
@RequiredArgsConstructor
public class SupportController {

  private final SupportChannels supportChannels;
  private final AntifraudContent antifraudContent;
  private final FaqSearch faqSearch;
  private final LibrasRequests librasRequests;
  private final UserContextProvider userContext;

  /** The channel cards, in content-defined order (BR1). */
  @GetMapping("/channels")
  List<SupportChannelView> channels() {
    return supportChannels.list();
  }

  /** The antifraud section content (BR3). */
  @GetMapping("/antifraud")
  AntifraudView antifraud() {
    return antifraudContent.content();
  }

  /** FAQ entries filtered by category and/or search term (BR5/BR6). */
  @GetMapping("/faq")
  List<FaqQuestionView> faq(
      @RequestParam(required = false) String category, @RequestParam(required = false) String q) {
    return faqSearch.search(category, q);
  }

  /** Registers a Central de Libras service request for the given beneficiary (BR4). */
  @PostMapping("/libras-requests")
  @ResponseStatus(HttpStatus.CREATED)
  LibrasRequestResponse requestLibras(@Valid @RequestBody LibrasRequestInput input) {
    return librasRequests.register(
        callerCard(), authorEmail(), input.beneficiaryId(), HttpRequestMetadata.current());
  }

  private String callerCard() {
    return userContext.current().beneficiaryCard().orElse(null);
  }

  private String authorEmail() {
    return userContext.current().username();
  }
}
