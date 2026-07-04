package com.fkmed.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import java.net.CookieManager;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * SPEC-0001 BR8 + AC3 (API side), end to end over the real embedded Authorization Server: the
 * dev-seam login (maria/dev12345) goes through the OIDC Authorization Code + PKCE flow, the issued
 * access token carries MARIA's beneficiary card, and {@code /api/plan/my-plan} answers the
 * canonical payload. Also proves the JDBC persistence of the AS state (V2).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("dev")
class LoginAndTokenFlowIT {

  @ServiceConnection static final PostgreSQLContainer POSTGRES = SharedPostgres.INSTANCE;

  private static final Pattern CSRF_INPUT = Pattern.compile("name=\"_csrf\"\\s+value=\"([^\"]+)\"");

  @LocalServerPort private int port;

  @Test
  void devLogin_authorizationCodeWithPkce_yieldsTokenBoundToMaria_andMyPlanAnswers()
      throws Exception {
    String base = "http://localhost:" + port;
    CookieManager cookies = new CookieManager();
    HttpClient client =
        HttpClient.newBuilder()
            .cookieHandler(cookies)
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    String verifier = randomVerifier();
    String challenge = s256(verifier);
    String authorizeUrl =
        base
            + "/oauth2/authorize?response_type=code&client_id=fkmed-web"
            + "&redirect_uri="
            + url("http://localhost:4200/")
            + "&scope="
            + url("openid profile")
            + "&state=fkmed-e2e&code_challenge="
            + challenge
            + "&code_challenge_method=S256";

    // 1. Unauthenticated authorize request is sent to the pt-BR login page.
    HttpResponse<String> toLogin = get(client, authorizeUrl);
    assertThat(toLogin.statusCode()).isEqualTo(302);
    assertThat(header(toLogin, "Location")).contains("/login");

    HttpResponse<String> loginPage = get(client, base + "/login");
    assertThat(loginPage.statusCode()).isEqualTo(200);
    assertThat(loginPage.body()).contains("Entrar no FKMed").contains("Usuário").contains("Senha");
    Matcher csrf = CSRF_INPUT.matcher(loginPage.body());
    assertThat(csrf.find()).as("login page must render the CSRF field").isTrue();

    // 2. Form login with the dev-seam credentials resumes the authorize request.
    HttpResponse<String> login =
        client.send(
            HttpRequest.newBuilder(URI.create(base + "/login"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(
                    HttpRequest.BodyPublishers.ofString(
                        "username=maria&password=dev12345&_csrf=" + url(csrf.group(1))))
                .build(),
            HttpResponse.BodyHandlers.ofString());
    assertThat(login.statusCode()).isEqualTo(302);
    assertThat(header(login, "Location")).doesNotContain("error");

    HttpResponse<String> authorized = get(client, header(login, "Location"));
    assertThat(authorized.statusCode()).isEqualTo(302);
    String redirect = header(authorized, "Location");
    assertThat(redirect).startsWith("http://localhost:4200/").contains("code=");
    String code = extractParam(redirect, "code");

    // 3. PKCE code exchange (public client, no secret).
    HttpResponse<String> token =
        client.send(
            HttpRequest.newBuilder(URI.create(base + "/oauth2/token"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(
                    HttpRequest.BodyPublishers.ofString(
                        "grant_type=authorization_code&client_id=fkmed-web&code="
                            + url(code)
                            + "&redirect_uri="
                            + url("http://localhost:4200/")
                            + "&code_verifier="
                            + verifier))
                .build(),
            HttpResponse.BodyHandlers.ofString());
    assertThat(token.statusCode()).isEqualTo(200);
    String accessToken = JsonPath.read(token.body(), "$.access_token");
    assertThat(accessToken).isNotBlank();
    assertThat((String) JsonPath.read(token.body(), "$.id_token")).isNotBlank();
    assertThat(token.body()).doesNotContain("refresh_token");

    // 4. The token authorizes the Meu Plano API and resolves MARIA's family (SPEC I/O example).
    HttpResponse<String> myPlan =
        client.send(
            HttpRequest.newBuilder(URI.create(base + "/api/plan/my-plan"))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build(),
            HttpResponse.BodyHandlers.ofString());
    assertThat(myPlan.statusCode()).isEqualTo(200);
    assertThat((String) JsonPath.read(myPlan.body(), "$.plan.name"))
        .isEqualTo("PLANO MÉDICO — ADESÃO PRATA RJ QP COPART TP");
    assertThat((String) JsonPath.read(myPlan.body(), "$.plan.ansRegistration")).isEqualTo("326305");
    assertThat((String) JsonPath.read(myPlan.body(), "$.members[0].fullName"))
        .isEqualTo("MARIA CLARA SOUZA LIMA");
    assertThat((String) JsonPath.read(myPlan.body(), "$.members[1].fullName"))
        .isEqualTo("PEDRO SOUZA LIMA");
  }

  private static HttpResponse<String> get(HttpClient client, String url) throws Exception {
    return client.send(
        HttpRequest.newBuilder(URI.create(url)).GET().build(),
        HttpResponse.BodyHandlers.ofString());
  }

  private static String header(HttpResponse<?> response, String name) {
    return response.headers().firstValue(name).orElseThrow();
  }

  private static String extractParam(String uri, String name) {
    for (String pair : URI.create(uri).getQuery().split("&")) {
      String[] kv = pair.split("=", 2);
      if (kv[0].equals(name)) {
        return java.net.URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
      }
    }
    throw new IllegalStateException("no query param '" + name + "' in " + uri);
  }

  private static String url(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8);
  }

  private static String randomVerifier() {
    byte[] bytes = new byte[32];
    new SecureRandom().nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  private static String s256(String verifier) throws Exception {
    byte[] digest =
        MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.US_ASCII));
    return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
  }
}
