package com.fkmed.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.CookieManager;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * SPEC-0002 slice 1.2 security surface over the REAL embedded Authorization Server form-login chain
 * and real Spring Session JDBC (Testcontainers Postgres) — the same quality bar as {@link
 * LoginSecurityAndLogoutAuditIT}: lockout timing (BR8/AC4) with a deterministic {@link
 * MutableClock}, BR7 neutrality (a non-existent e-mail never locks and never creates a row), the
 * two BR12 idle windows (AC7 — persisted {@code max_inactive_interval} + cookie {@code Max-Age})
 * and a password reset terminating every active session (BR10/AC6).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("dev")
@Import({AccountLockoutAndSessionIT.FixedClockConfig.class, RecordingMailConfig.class})
class AccountLockoutAndSessionIT {

  @ServiceConnection static final PostgreSQLContainer POSTGRES = SharedPostgres.INSTANCE;

  private static final Pattern CSRF_INPUT = Pattern.compile("name=\"_csrf\"\\s+value=\"([^\"]+)\"");
  private static final Pattern RESET_TOKEN = Pattern.compile("token=([A-Za-z0-9\\-_%]+)");
  static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
  static final Instant START = Instant.parse("2026-07-04T12:00:00Z");

  private static final String MARIA_EMAIL = "maria@fkmed.local";
  private static final String ACCOUNT_EMAIL = "security-it@fkmed.local";
  private static final String ACCOUNT_PASSWORD = "Senha1234";
  private static final String PEDRO_BENEFICIARY_ID = "9c8b7a6d-5e4f-4a3b-8c2d-1e0f9a8b7c6d";

  @LocalServerPort private int port;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private MutableClock clock;
  @Autowired private RecordingMailSender mail;

  private String base;

  @BeforeEach
  void setUp() {
    base = "http://localhost:" + port;
    clock.reset(START);
    clean();
    seedActiveAccount();
  }

  @AfterEach
  void tearDown() {
    clean();
  }

  private void clean() {
    jdbc.update("delete from spring_session_attributes");
    jdbc.update("delete from spring_session");
    jdbc.update("delete from audit_event");
    jdbc.update("delete from password_reset_token");
    jdbc.update("delete from email_verification_token");
    jdbc.update("delete from term_acceptance");
    jdbc.update("delete from user_account where email <> ?", MARIA_EMAIL);
    // MARIA is reset too so a prior class's failed attempt can never leave her locked here.
    jdbc.update("update user_account set failed_attempts = 0, locked_until = null");
    mail.messages.clear();
  }

  @Test
  void ac4_fiveFailures_lock15Min_correctPasswordRefused_thenSucceedsAfterWindow()
      throws Exception {
    for (int attempt = 1; attempt <= 5; attempt++) {
      HttpResponse<String> failure = login(newClient(), ACCOUNT_EMAIL, "wrong-pass-" + attempt);
      assertThat(header(failure, "Location")).endsWith("/login?error");
    }
    assertThat(failedAttempts(ACCOUNT_EMAIL)).isEqualTo(5);
    assertThat(lockedUntil(ACCOUNT_EMAIL)).isNotNull();
    assertThat(auditCount("identity.account-locked")).isEqualTo(1);

    // 6th attempt with the CORRECT password, still inside the 15-minute window → locked, not
    // success.
    HttpResponse<String> whileLocked = login(newClient(), ACCOUNT_EMAIL, ACCOUNT_PASSWORD);
    assertThat(header(whileLocked, "Location")).endsWith("/login?locked");
    // The refused attempt during the lock did not extend it, nor re-audit it.
    assertThat(auditCount("identity.account-locked")).isEqualTo(1);

    // After the 15-minute window a correct password succeeds and clears the counter.
    clock.advance(Duration.ofMinutes(16));
    HttpResponse<String> afterWindow = login(newClient(), ACCOUNT_EMAIL, ACCOUNT_PASSWORD);
    assertThat(header(afterWindow, "Location")).doesNotContain("error").doesNotContain("locked");
    assertThat(failedAttempts(ACCOUNT_EMAIL)).isZero();
    assertThat(lockedUntil(ACCOUNT_EMAIL)).isNull();
  }

  @Test
  void br7_nonexistentEmail_neverLocks_createsNoRow_andStaysNeutral() throws Exception {
    // Five+ failures on a non-existent e-mail must never produce a lock state or a row (BR7): the
    // lock is only for real accounts that actually reached 5 failures.
    for (int attempt = 1; attempt <= 6; attempt++) {
      HttpResponse<String> ghost = login(newClient(), "ghost@fkmed.local", "whatever-" + attempt);
      assertThat(header(ghost, "Location")).endsWith("/login?error");
    }
    assertThat(rowCount("ghost@fkmed.local")).isZero();

    // A wrong password on a real UNLOCKED account is byte-identical to the non-existent case.
    HttpResponse<String> wrongOnReal = login(newClient(), ACCOUNT_EMAIL, "wrong-once");
    HttpResponse<String> nonexistent = login(newClient(), "nobody@fkmed.local", "wrong-once");
    assertThat(nonexistent.statusCode()).isEqualTo(wrongOnReal.statusCode());
    assertThat(header(nonexistent, "Location")).isEqualTo(header(wrongOnReal, "Location"));
    assertThat(nonexistent.body()).isEqualTo(wrongOnReal.body());
  }

  @Test
  void ac7_keepConnectedUnchecked_yieldsASessionCookie_and30MinuteIdleWindow() throws Exception {
    HttpResponse<String> login = login(newClient(), ACCOUNT_EMAIL, ACCOUNT_PASSWORD, false);
    assertThat(header(login, "Location")).doesNotContain("error");

    assertThat(maxInactiveInterval(ACCOUNT_EMAIL)).isEqualTo(1800);
    assertThat(sessionCookie(login)).isPresent().get().asString().doesNotContain("Max-Age");
  }

  @Test
  void ac7_keepConnectedChecked_yieldsAPersistentCookie_and7DayIdleWindow() throws Exception {
    HttpResponse<String> login = login(newClient(), ACCOUNT_EMAIL, ACCOUNT_PASSWORD, true);
    assertThat(header(login, "Location")).doesNotContain("error");

    // The 7-day idle window lives in the server-side session; the cookie is merely made persistent
    // (a long Max-Age) so it survives a browser restart (ADR-0005).
    assertThat(maxInactiveInterval(ACCOUNT_EMAIL)).isEqualTo(604800);
    assertThat(sessionCookie(login)).isPresent().get().asString().contains("Max-Age=");
  }

  @Test
  void ac6_passwordReset_terminatesEveryActiveSessionOfTheUser() throws Exception {
    login(newClient(), ACCOUNT_EMAIL, ACCOUNT_PASSWORD);
    login(newClient(), ACCOUNT_EMAIL, ACCOUNT_PASSWORD);
    assertThat(sessionCountFor(ACCOUNT_EMAIL)).isEqualTo(2);

    postJson("/api/auth/recovery/request", "{\"email\":\"" + ACCOUNT_EMAIL + "\"}");
    String resetToken = extractResetToken();
    HttpResponse<String> reset =
        postJson(
            "/api/auth/recovery/reset",
            "{\"token\":\"" + resetToken + "\",\"newPassword\":\"BrandNew123\"}");
    assertThat(reset.statusCode()).isEqualTo(200);

    assertThat(sessionCountFor(ACCOUNT_EMAIL)).isZero();
  }

  private void seedActiveAccount() {
    jdbc.update(
        "insert into user_account (id, beneficiary_id, email, password_hash, status,"
            + " failed_attempts, created_at) values (gen_random_uuid(), ?::uuid, ?,"
            + " '{bcrypt}' || crypt(?, gen_salt('bf', 10)), 'ACTIVE', 0, now())",
        PEDRO_BENEFICIARY_ID,
        ACCOUNT_EMAIL,
        ACCOUNT_PASSWORD);
  }

  private String extractResetToken() {
    assertThat(mail.messages).as("a reset e-mail must have been sent").isNotEmpty();
    Matcher matcher = RESET_TOKEN.matcher(mail.messages.getLast().body());
    assertThat(matcher.find()).as("the reset e-mail must carry a token link").isTrue();
    return java.net.URLDecoder.decode(matcher.group(1), StandardCharsets.UTF_8);
  }

  private HttpResponse<String> login(HttpClient client, String email, String password)
      throws Exception {
    return login(client, email, password, false);
  }

  private HttpResponse<String> login(
      HttpClient client, String email, String password, boolean rememberMe) throws Exception {
    HttpResponse<String> loginPage = get(client, base + "/login");
    Matcher csrf = CSRF_INPUT.matcher(loginPage.body());
    assertThat(csrf.find()).as("login page must render the CSRF field").isTrue();
    String body =
        "username="
            + url(email)
            + "&password="
            + url(password)
            + "&_csrf="
            + url(csrf.group(1))
            + (rememberMe ? "&remember-me=on" : "");
    return client.send(
        HttpRequest.newBuilder(URI.create(base + "/login"))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build(),
        HttpResponse.BodyHandlers.ofString());
  }

  private HttpResponse<String> postJson(String path, String json) throws Exception {
    return newClient()
        .send(
            HttpRequest.newBuilder(URI.create(base + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build(),
            HttpResponse.BodyHandlers.ofString());
  }

  private static Optional<String> sessionCookie(HttpResponse<?> response) {
    List<String> cookies = response.headers().allValues("Set-Cookie");
    return cookies.stream().filter(cookie -> cookie.startsWith("SESSION=")).findFirst();
  }

  private int failedAttempts(String email) {
    return jdbc.queryForObject(
        "select failed_attempts from user_account where email = ?", Integer.class, email);
  }

  private Instant lockedUntil(String email) {
    return jdbc.queryForObject(
        "select locked_until from user_account where email = ?", Instant.class, email);
  }

  private int maxInactiveInterval(String principal) {
    return jdbc.queryForObject(
        "select max_inactive_interval from spring_session where principal_name = ?",
        Integer.class,
        principal);
  }

  private long sessionCountFor(String principal) {
    return jdbc.queryForObject(
        "select count(*) from spring_session where principal_name = ?", Long.class, principal);
  }

  private long rowCount(String email) {
    return jdbc.queryForObject(
        "select count(*) from user_account where email = ?", Long.class, email);
  }

  private long auditCount(String eventType) {
    return jdbc.queryForObject(
        "select count(*) from audit_event where event_type = ?", Long.class, eventType);
  }

  private static HttpClient newClient() {
    return HttpClient.newBuilder()
        .cookieHandler(new CookieManager())
        .followRedirects(HttpClient.Redirect.NEVER)
        .build();
  }

  private static HttpResponse<String> get(HttpClient client, String url) throws Exception {
    return client.send(
        HttpRequest.newBuilder(URI.create(url)).GET().build(),
        HttpResponse.BodyHandlers.ofString());
  }

  private static String header(HttpResponse<?> response, String name) {
    return response.headers().firstValue(name).orElseThrow();
  }

  private static String url(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8);
  }

  /** Overrides the application {@code Clock} with a test-advanceable one (BR8/BR10 timing). */
  @TestConfiguration
  static class FixedClockConfig {
    @Bean
    @Primary
    MutableClock mutableClock() {
      return new MutableClock(START, ZONE);
    }
  }
}
