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
    messages.addMessage("access.denied", ptBr, "Você não tem permissão para acessar este recurso.");
    messages.addMessage("auth.concurrent-update", ptBr, "Acesso simultâneo — tente novamente.");
    handler = new GlobalExceptionHandler(messages);
  }

  @Test
  void frameworkErrorResponse_keepsItsOwnStatus_neverBecomes500() {
    // Regression (review finding I1): unknown route must stay 404, wrong method 405.
    ResponseEntity<ApiErrorResponse> notFound =
        handler.handleUnexpected(
            new org.springframework.web.servlet.resource.NoResourceFoundException(
                org.springframework.http.HttpMethod.GET, "/api/unknown-route", null));
    assertThat(notFound.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(notFound.getBody().code()).isEqualTo("http.404");

    ResponseEntity<ApiErrorResponse> methodNotAllowed =
        handler.handleUnexpected(
            new org.springframework.web.HttpRequestMethodNotSupportedException("POST"));
    assertThat(methodNotAllowed.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
    assertThat(methodNotAllowed.getBody().code()).isEqualTo("http.405");
  }

  @Test
  void accessDenied_becomes403_withLocalizedMessage() {
    ResponseEntity<ApiErrorResponse> response =
        handler.handleAccessDenied(
            new org.springframework.security.access.AccessDeniedException("denied"));
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(response.getBody().code()).isEqualTo("access.denied");
    assertThat(response.getBody().message())
        .isEqualTo("Você não tem permissão para acessar este recurso.");
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
  void optimisticLockConflict_isTranslatedTo409_domainErrorContract_notARawFrameworkException() {
    // Débito técnico A (DL-0005): the raw framework exception must never leak; it renders as the
    // domain auth.concurrent-update / 409 contract, retryable by the client.
    ResponseEntity<ApiErrorResponse> response =
        handler.handleOptimisticLock(
            new org.springframework.orm.ObjectOptimisticLockingFailureException(
                Object.class, java.util.UUID.randomUUID()));
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(response.getBody().code()).isEqualTo("auth.concurrent-update");
    assertThat(response.getBody().message()).isEqualTo("Acesso simultâneo — tente novamente.");
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
