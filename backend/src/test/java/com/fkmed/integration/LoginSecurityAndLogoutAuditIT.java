package com.fkmed.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.CookieManager;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * SPEC-0002 BR6/BR7/BR14, end to end over the real embedded Authorization Server form-login chain —
 * exactly the security-sensitive surface QA flagged as relying on an unverified default: without a
 * committed test, Spring Security's own {@code hideUserNotFoundExceptions} collapsing
 * wrong-password and nonexistent-e-mail into the same failure could regress silently (e.g. a future
 * custom {@code AuthenticationProvider} that forgets to preserve it), and logout leaving no audit
 * trace would go unnoticed.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("dev")
class LoginSecurityAndLogoutAuditIT {

  @ServiceConnection static final PostgreSQLContainer POSTGRES = SharedPostgres.INSTANCE;

  private static final Pattern CSRF_INPUT = Pattern.compile("name=\"_csrf\"\\s+value=\"([^\"]+)\"");
  private static final String MARIA_EMAIL = "maria@fkmed.local";
  private static final String MARIA_PASSWORD = "maria12345";

  @LocalServerPort private int port;
  @Autowired private JdbcTemplate jdbc;

  private String base;

  @BeforeEach
  void setUp() {
    base = "http://localhost:" + port;
    // Isolation: this class creates a PEDRO account (BR6 test) and login/logout audit rows.
    jdbc.update(
        "delete from audit_event where event_type in"
            + " ('identity.login-success','identity.login-failure','identity.logout')");
    jdbc.update("delete from email_verification_token");
    jdbc.update("delete from term_acceptance");
    jdbc.update("delete from user_account where email <> ?", MARIA_EMAIL);
  }

  @Test
  void br7_wrongPasswordAndNonexistentEmail_yieldByteIdenticalFailureResponses() throws Exception {
    HttpResponse<String> wrongPassword =
        attemptLogin(newClient(), MARIA_EMAIL, "definitely-the-wrong-password");
    HttpResponse<String> nonexistentEmail =
        attemptLogin(newClient(), "nobody-registered@fkmed.local", "whatever-password-1");

    // BR7: never revealing whether the e-mail exists or which credential failed — the two
    // responses must be indistinguishable to the caller.
    assertThat(wrongPassword.statusCode()).isEqualTo(302);
    assertThat(nonexistentEmail.statusCode()).isEqualTo(wrongPassword.statusCode());
    assertThat(header(wrongPassword, "Location")).endsWith("/login?error");
    assertThat(header(nonexistentEmail, "Location")).isEqualTo(header(wrongPassword, "Location"));
    assertThat(nonexistentEmail.body()).isEqualTo(wrongPassword.body());
  }

  @Test
  void br6_unverifiedAccount_getsTheDistinctUnverifiedRedirect() throws Exception {
    registerPedroWithoutVerifying();

    HttpResponse<String> attempt = attemptLogin(newClient(), "pedro@fkmed.local", "Pedro1234");

    // BR6: distinct from the generic BR7 refusal — a specific message + resend affordance.
    assertThat(attempt.statusCode()).isEqualTo(302);
    assertThat(header(attempt, "Location")).endsWith("/login?unverified");
  }

  @Test
  void br14_logout_recordsAnAuditEvent() throws Exception {
    HttpClient client = newClient();
    HttpResponse<String> login = attemptLogin(client, MARIA_EMAIL, MARIA_PASSWORD);
    assertThat(login.statusCode()).isEqualTo(302);
    assertThat(header(login, "Location")).doesNotContain("error");

    long before = countAuditEvents("identity.logout");

    // A fresh CSRF token bound to the now-authenticated session, then the logout POST.
    HttpResponse<String> authenticatedPage = get(client, base + "/login");
    Matcher csrf = CSRF_INPUT.matcher(authenticatedPage.body());
    assertThat(csrf.find()).as("an authenticated page must still render a CSRF field").isTrue();

    HttpResponse<String> logout =
        client.send(
            HttpRequest.newBuilder(URI.create(base + "/logout"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString("_csrf=" + url(csrf.group(1))))
                .build(),
            HttpResponse.BodyHandlers.ofString());
    assertThat(logout.statusCode()).isEqualTo(302);

    assertThat(countAuditEvents("identity.logout")).isEqualTo(before + 1);
  }

  private long countAuditEvents(String eventType) {
    return jdbc.queryForObject(
        "select count(*) from audit_event where event_type = ?", Long.class, eventType);
  }

  /** Registers PEDRO's real account through the API but never confirms the e-mail (BR6 fixture). */
  private void registerPedroWithoutVerifying() throws Exception {
    HttpClient client = newClient();
    HttpResponse<String> verify =
        client.send(
            HttpRequest.newBuilder(URI.create(base + "/api/auth/first-access/verify"))
                .header("Content-Type", "application/json")
                .POST(
                    HttpRequest.BodyPublishers.ofString(
                        "{\"cpf\":\"15350946056\",\"cardNumber\":\"001234575\","
                            + "\"birthDate\":\"2007-05-20\"}"))
                .build(),
            HttpResponse.BodyHandlers.ofString());
    assertThat(verify.statusCode()).isEqualTo(200);
    String registrationToken = extractJson(verify.body(), "registrationToken");

    HttpResponse<String> complete =
        client.send(
            HttpRequest.newBuilder(URI.create(base + "/api/auth/first-access/complete"))
                .header("Content-Type", "application/json")
                .POST(
                    HttpRequest.BodyPublishers.ofString(
                        ("{\"registrationToken\":\"%s\",\"email\":\"pedro@fkmed.local\","
                                + "\"password\":\"Pedro1234\",\"acceptedTerms\":true,"
                                + "\"acceptedPrivacy\":true}")
                            .formatted(registrationToken)))
                .build(),
            HttpResponse.BodyHandlers.ofString());
    assertThat(complete.statusCode()).isEqualTo(201);
  }

  private static String extractJson(String body, String field) {
    Matcher matcher = Pattern.compile("\"" + field + "\"\\s*:\\s*\"([^\"]+)\"").matcher(body);
    assertThat(matcher.find()).as("field '%s' must be present in %s", field, body).isTrue();
    return matcher.group(1);
  }

  private HttpResponse<String> attemptLogin(HttpClient client, String email, String password)
      throws Exception {
    HttpResponse<String> loginPage = get(client, base + "/login");
    Matcher csrf = CSRF_INPUT.matcher(loginPage.body());
    assertThat(csrf.find()).as("login page must render the CSRF field").isTrue();

    return client.send(
        HttpRequest.newBuilder(URI.create(base + "/login"))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(
                HttpRequest.BodyPublishers.ofString(
                    "username="
                        + url(email)
                        + "&password="
                        + url(password)
                        + "&_csrf="
                        + url(csrf.group(1))))
            .build(),
        HttpResponse.BodyHandlers.ofString());
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
}
