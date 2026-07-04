package com.fkmed.infra.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.fkmed.domain.plan.PlanNotFoundException;
import java.util.Locale;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/** DECISIONS-BASELINE §0011: domain errors render the registry status + pt-BR message. */
class GlobalExceptionHandlerTest {

  private GlobalExceptionHandler handler;

  @BeforeEach
  void setUp() {
    StaticMessageSource messages = new StaticMessageSource();
    Locale ptBr = Locale.forLanguageTag("pt-BR");
    messages.addMessage(
        "plan.not-found", ptBr, "Nenhum plano vinculado ao seu usuário foi encontrado.");
    messages.addMessage("internal.error", ptBr, "Ocorreu um erro inesperado.");
    handler = new GlobalExceptionHandler(messages);
  }

  @Test
  void domainException_rendersRegisteredStatus_andLocalizedMessage() {
    ResponseEntity<ApiErrorResponse> response =
        handler.handleDomainException(new PlanNotFoundException());
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(response.getBody().code()).isEqualTo("plan.not-found");
    assertThat(response.getBody().message())
        .isEqualTo("Nenhum plano vinculado ao seu usuário foi encontrado.");
    assertThat(response.getBody().fields()).isEmpty();
  }

  @Test
  void unexpectedException_becomesOpaque500_withoutLeakingInternals() {
    ResponseEntity<ApiErrorResponse> response =
        handler.handleUnexpected(new IllegalStateException("secret internals"));
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    assertThat(response.getBody().code()).isEqualTo("internal.error");
    assertThat(response.getBody().message()).doesNotContain("secret internals");
  }
}
