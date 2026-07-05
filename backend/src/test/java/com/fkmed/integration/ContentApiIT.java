package com.fkmed.integration;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

/**
 * SPEC-0005 BR6/BR7/BR9: {@code GET /api/content/home} returns only visible banners (active +
 * inside their validity window) and active notices, in content-defined order.
 *
 * <p>The {@code banner}/{@code notice} tables carry the permanent BR9 seed (V8), which every test
 * in this class leaves untouched. Extra rows used to exercise the validity-window edge cases are
 * tagged with {@link #FIXTURE_MARK} and cleaned in both {@code @BeforeEach} (in case a previous run
 * crashed before its own cleanup) and {@code @AfterEach}, per the shared-Postgres isolation rule
 * (docs/architecture/testing.md).
 */
class ContentApiIT extends AbstractIntegrationTest {

  private static final String FIXTURE_MARK = "IT-FIXTURE-";

  @Autowired private MockMvc mockMvc;
  @Autowired private JdbcTemplate jdbc;

  @BeforeEach
  @AfterEach
  void cleanFixtureRows() {
    jdbc.update("delete from banner where title like ?", FIXTURE_MARK + "%");
    jdbc.update("delete from notice where title like ?", FIXTURE_MARK + "%");
  }

  @Test
  void homeContent_withoutAuthentication_returns401() throws Exception {
    mockMvc.perform(get("/api/content/home")).andExpect(status().isUnauthorized());
  }

  @Test
  void homeContent_returnsTheBr9SeedInOrder_whenNoExtraContentExists() throws Exception {
    mockMvc
        .perform(get("/api/content/home").with(jwt()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.banners.length()").value(2))
        .andExpect(jsonPath("$.banners[0].title").value("Alerta de golpe!"))
        .andExpect(jsonPath("$.banners[0].buttonLabel").value("Saiba mais"))
        .andExpect(jsonPath("$.banners[0].destination").value("/atendimento#antifraude"))
        .andExpect(jsonPath("$.banners[0].imageUrl").doesNotExist())
        .andExpect(jsonPath("$.banners[0].order").value(1))
        .andExpect(jsonPath("$.banners[1].title").value("Valide seu boleto"))
        .andExpect(jsonPath("$.banners[1].order").value(2))
        .andExpect(jsonPath("$.notices.length()").value(2))
        .andExpect(jsonPath("$.notices[0].title").value("Instabilidade momentânea da Telemedicina"))
        .andExpect(jsonPath("$.notices[0].severity").value("ALERT"))
        .andExpect(jsonPath("$.notices[0].order").value(1))
        .andExpect(jsonPath("$.notices[1].title").value("Lei Geral de Proteção de Dados Pessoais"))
        .andExpect(jsonPath("$.notices[1].severity").value("INFORMATIVE"))
        .andExpect(jsonPath("$.notices[1].order").value(2));
  }

  @Test
  void homeContent_excludesInactiveAndOutOfWindowBanners_includesValidOnesInOrder()
      throws Exception {
    insertBanner(FIXTURE_MARK + "inactive", 90, false, null, null);
    insertBanner(FIXTURE_MARK + "expired", 91, true, null, "now() - interval '1 day'");
    insertBanner(FIXTURE_MARK + "future", 92, true, "now() + interval '1 day'", null);
    insertBanner(FIXTURE_MARK + "no-window", 93, true, null, null);
    insertBanner(
        FIXTURE_MARK + "within-window",
        94,
        true,
        "now() - interval '1 day'",
        "now() + interval '1 day'");

    mockMvc
        .perform(get("/api/content/home").with(jwt()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.banners.length()").value(4))
        .andExpect(jsonPath("$.banners[0].title").value("Alerta de golpe!"))
        .andExpect(jsonPath("$.banners[1].title").value("Valide seu boleto"))
        .andExpect(jsonPath("$.banners[2].title").value(FIXTURE_MARK + "no-window"))
        .andExpect(jsonPath("$.banners[3].title").value(FIXTURE_MARK + "within-window"));
  }

  @Test
  void homeContent_excludesInactiveNotices_includesActiveOnesInOrder() throws Exception {
    insertNotice(FIXTURE_MARK + "inactive-notice", 90, false);
    insertNotice(FIXTURE_MARK + "active-notice", 91, true);

    mockMvc
        .perform(get("/api/content/home").with(jwt()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.notices.length()").value(3))
        .andExpect(jsonPath("$.notices[2].title").value(FIXTURE_MARK + "active-notice"));
  }

  /**
   * Inserts a fixture banner. {@code validFromExpr}/{@code validToExpr} are raw SQL expressions
   * (e.g. {@code "now() - interval '1 day'"}) or {@code null} for an open bound — safe to concat
   * here since both come only from the fixed literals in this test class, never external input.
   */
  private void insertBanner(
      String title, int displayOrder, boolean active, String validFromExpr, String validToExpr) {
    jdbc.update(
        "insert into banner (id, title, text, button_label, internal_destination, "
            + "display_order, active, valid_from, valid_to) values (gen_random_uuid(), ?, "
            + "'fixture text', 'Ver', '/fixture', ?, ?, "
            + (validFromExpr == null ? "null" : validFromExpr)
            + ", "
            + (validToExpr == null ? "null" : validToExpr)
            + ")",
        title,
        displayOrder,
        active);
  }

  private void insertNotice(String title, int displayOrder, boolean active) {
    jdbc.update(
        "insert into notice (id, title, body, severity, display_order, active) values "
            + "(gen_random_uuid(), ?, 'fixture body', 'INFORMATIVE', ?, ?)",
        title,
        displayOrder,
        active);
  }
}
