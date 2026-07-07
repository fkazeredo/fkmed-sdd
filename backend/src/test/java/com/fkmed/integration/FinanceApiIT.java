package com.fkmed.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * SPEC-0013 beneficiary API over MockMvc + Testcontainers Postgres against the V24 seed: the tabs
 * (open+overdue / paid, derived status + overdue update — BR2), detail + second-copy PDF (BR3), the
 * antifraud validator against the seeded lines (BR4), the copay statement filters + total (BR5),
 * the IR/settlement years and PDFs (BR6/BR7) and the titular-only 403 for a dependent (BR1).
 * Read-only over the seed (no cleanup needed — the sim IT owns the write-path rows).
 */
class FinanceApiIT extends AbstractIntegrationTest {

  private static final String MARIA_EMAIL = "maria@fkmed.local";
  private static final int PRIOR_YEAR = LocalDate.now().getYear() - 1;
  private static final int CURRENT_YEAR = LocalDate.now().getYear();
  private static final String OPEN_INVOICE = "fa000000-0000-4000-8000-000000000003";
  private static final String PAID_INVOICE = "fa000000-0000-4000-8000-000000000001";
  private static final String OPEN_LINE = "34191098765000432019874561230987650000000000003";
  private static final String PEDRO_ID = "9c8b7a6d-5e4f-4a3b-8c2d-1e0f9a8b7c6d";

  @Autowired private MockMvc mockMvc;

  @Test
  void openTab_showsOpenAndOverdue_orderedByDueAsc_overdueCarriesTheUpdatedAmount()
      throws Exception {
    mockMvc
        .perform(get("/api/finance/invoices").param("tab", "OPEN").with(authAs(MARIA_EMAIL)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].status").value("OVERDUE"))
        .andExpect(jsonPath("$[0].updatedAmount.original").value(489.90))
        .andExpect(jsonPath("$[0].updatedAmount.fine").value(9.80))
        .andExpect(jsonPath("$[0].updatedAmount.daysOverdue").isNumber())
        .andExpect(jsonPath("$[1].status").value("OPEN"))
        .andExpect(jsonPath("$[1].updatedAmount").doesNotExist());
  }

  @Test
  void paidTab_showsThePriorYearInvoices_competenciaDesc_withPaidAt() throws Exception {
    mockMvc
        .perform(get("/api/finance/invoices").param("tab", "PAID").with(authAs(MARIA_EMAIL)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].status").value("PAID"))
        .andExpect(jsonPath("$[0].paidAt").isNotEmpty())
        .andExpect(jsonPath("$[0].competencia").value("Setembro/" + PRIOR_YEAR))
        .andExpect(jsonPath("$[1].competencia").value("Maio/" + PRIOR_YEAR));
  }

  @Test
  void detail_carriesTheDigitableLinePixAndBarcode() throws Exception {
    mockMvc
        .perform(get("/api/finance/invoices/{id}", OPEN_INVOICE).with(authAs(MARIA_EMAIL)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.digitableLine").value(OPEN_LINE))
        .andExpect(jsonPath("$.pixCode").isNotEmpty())
        .andExpect(jsonPath("$.barcodePayload", org.hamcrest.Matchers.matchesPattern("\\d{44}")));
  }

  @Test
  void detail_ofAnUnknownInvoice_is404() throws Exception {
    mockMvc
        .perform(
            get("/api/finance/invoices/{id}", "fa000000-0000-4000-8000-0000000000ff")
                .with(authAs(MARIA_EMAIL)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("finance.invoice-not-found"));
  }

  @Test
  void invoicePdf_isAPdf() throws Exception {
    byte[] pdf =
        mockMvc
            .perform(get("/api/finance/invoices/{id}/pdf", PAID_INVOICE).with(authAs(MARIA_EMAIL)))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_PDF))
            .andReturn()
            .getResponse()
            .getContentAsByteArray();
    assertThat(new String(pdf, 0, 5, java.nio.charset.StandardCharsets.US_ASCII))
        .isEqualTo("%PDF-");
  }

  @Test
  void validator_authentic_forASeededLineWithSpaces() throws Exception {
    String spaced = OPEN_LINE.replaceAll("(.{5})", "$1 ").trim();
    mockMvc
        .perform(
            post("/api/finance/invoices/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"line\":\"" + spaced + "\"}")
                .with(authAs(MARIA_EMAIL)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.result").value("AUTHENTIC"))
        .andExpect(jsonPath("$.competencia").isNotEmpty())
        .andExpect(jsonPath("$.amount").value(489.90));
  }

  @Test
  void validator_notRecognized_forAnUnknown47DigitLine() throws Exception {
    mockMvc
        .perform(
            post("/api/finance/invoices/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"line\":\"11111111111111111111111111111111111111111111111\"}")
                .with(authAs(MARIA_EMAIL)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.result").value("NOT_RECOGNIZED"))
        .andExpect(jsonPath("$.competencia").doesNotExist());
  }

  @Test
  void validator_rejectsANonFortySevenDigitLine_with422() throws Exception {
    mockMvc
        .perform(
            post("/api/finance/invoices/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"line\":\"123456789012345678901234567890\"}")
                .with(authAs(MARIA_EMAIL)))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("finance.line-invalid-format"));
  }

  @Test
  void copay_wholeFamily_totalsExactlyTheReturnedEntries_andTheBeneficiaryFilterNarrows()
      throws Exception {
    LocalDate to = LocalDate.now();
    LocalDate from = to.minusDays(100);
    mockMvc
        .perform(
            get("/api/finance/copay")
                .param("period", "CUSTOM")
                .param("from", from.toString())
                .param("to", to.toString())
                .with(authAs(MARIA_EMAIL)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.entries.length()").value(8))
        .andExpect(jsonPath("$.total").value(293.50));

    mockMvc
        .perform(
            get("/api/finance/copay")
                .param("period", "CUSTOM")
                .param("from", from.toString())
                .param("to", to.toString())
                .param("beneficiaryId", PEDRO_ID)
                .with(authAs(MARIA_EMAIL)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.entries.length()").value(3))
        .andExpect(jsonPath("$.total").value(110.00))
        .andExpect(jsonPath("$.entries[0].beneficiaryName").value("PEDRO"));
  }

  @Test
  void taxStatements_listsThePriorYear_andRendersItsPdf() throws Exception {
    mockMvc
        .perform(get("/api/finance/tax-statements").with(authAs(MARIA_EMAIL)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].year").value(PRIOR_YEAR));

    mockMvc
        .perform(
            get("/api/finance/tax-statements/{year}/pdf", PRIOR_YEAR).with(authAs(MARIA_EMAIL)))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_PDF));
  }

  @Test
  void settlementDeclarations_offerThePriorYear_andRejectTheCurrentYear() throws Exception {
    mockMvc
        .perform(get("/api/finance/settlement-declarations").with(authAs(MARIA_EMAIL)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].year").value(PRIOR_YEAR));

    mockMvc
        .perform(
            get("/api/finance/settlement-declarations/{year}/pdf", PRIOR_YEAR)
                .with(authAs(MARIA_EMAIL)))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_PDF));

    mockMvc
        .perform(
            get("/api/finance/settlement-declarations/{year}/pdf", CURRENT_YEAR)
                .with(authAs(MARIA_EMAIL)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("finance.year-not-settled"));
  }

  @Test
  void aDependentCaller_isDenied_403_titularOnly() throws Exception {
    seedPedroAccount();
    mockMvc
        .perform(
            get("/api/finance/invoices").param("tab", "OPEN").with(authAs("pedro@fkmed.local")))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("finance.titular-only"));
  }

  @Autowired private org.springframework.jdbc.core.JdbcTemplate jdbc;

  /**
   * PEDRO (dependent, 001234575) has no seeded login; create one so a dependent caller can be
   * authenticated (mirrors OperatorSimTeleIT's operator-credential seeding).
   */
  private void seedPedroAccount() {
    jdbc.update(
        "insert into user_account (id, beneficiary_id, email, password_hash, status, created_at)"
            + " values ('fa000000-0000-4000-8000-0000000000aa'::uuid, ?::uuid, 'pedro@fkmed.local',"
            + " '{noop}pedro12345', 'ACTIVE', now()) on conflict (email) do nothing",
        PEDRO_ID);
  }

  private static RequestPostProcessor authAs(String email) {
    return jwt().jwt(jwt -> jwt.subject(email));
  }
}
