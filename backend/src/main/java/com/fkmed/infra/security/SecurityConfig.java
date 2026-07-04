package com.fkmed.infra.security;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * The three ordered security filter chains of DECISIONS-BASELINE §0018: Authorization Server
 * endpoints, the {@code /api/**} resource server, and the form-login fallback.
 *
 * <p>SPEC-0001 BR1/BR3: only {@code /api/system/health} and {@code /api/system/version} are public;
 * every other API route answers {@code 401} to unauthenticated calls. {@code /actuator/prometheus}
 * and {@code /v3/api-docs/**} are app-level public but never routed by the production proxy
 * (internal network only — see SECURITY.md): the OpenAPI document is the committed, public contract
 * ({@code docs/api/openapi.json}) and the snapshot gate reads it unauthenticated; role-gating
 * arrives with SPEC-0002's role catalogue.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

  @Bean
  @Order(1)
  SecurityFilterChain authorizationServerChain(HttpSecurity http) throws Exception {
    OAuth2AuthorizationServerConfigurer authorizationServer =
        new OAuth2AuthorizationServerConfigurer();
    http.securityMatcher(authorizationServer.getEndpointsMatcher())
        .with(authorizationServer, server -> server.oidc(Customizer.withDefaults()))
        .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
        .csrf(csrf -> csrf.ignoringRequestMatchers(authorizationServer.getEndpointsMatcher()))
        .cors(Customizer.withDefaults())
        .exceptionHandling(
            exceptions ->
                exceptions.defaultAuthenticationEntryPointFor(
                    new LoginUrlAuthenticationEntryPoint("/login"),
                    new MediaTypeRequestMatcher(MediaType.TEXT_HTML)))
        .oauth2ResourceServer(resource -> resource.jwt(Customizer.withDefaults()));
    return http.build();
  }

  @Bean
  @Order(2)
  SecurityFilterChain apiChain(HttpSecurity http) throws Exception {
    http.securityMatcher("/api/**")
        .authorizeHttpRequests(
            authorize ->
                authorize
                    .requestMatchers("/api/system/health", "/api/system/version")
                    .permitAll()
                    // Public first-access + verification endpoints (SPEC-0002): the visitor is
                    // unauthenticated during registration and e-mail verification.
                    .requestMatchers("/api/auth/first-access/**", "/api/auth/verification/**")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .csrf(csrf -> csrf.disable())
        .cors(Customizer.withDefaults())
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .oauth2ResourceServer(resource -> resource.jwt(Customizer.withDefaults()));
    return http.build();
  }

  @Bean
  @Order(3)
  SecurityFilterChain formLoginChain(
      HttpSecurity http,
      UnverifiedAwareAuthenticationFailureHandler failureHandler,
      LogoutAuditRecorder logoutAuditRecorder)
      throws Exception {
    http.authorizeHttpRequests(
            authorize ->
                authorize
                    .requestMatchers(
                        "/login",
                        "/error",
                        "/assets/**",
                        "/favicon.ico",
                        "/v3/api-docs/**",
                        "/actuator/health",
                        "/actuator/prometheus")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .formLogin(form -> form.loginPage("/login").failureHandler(failureHandler).permitAll())
        .logout(logout -> logout.addLogoutHandler(logoutAuditRecorder).permitAll());
    return http.build();
  }

  @Bean
  PasswordEncoder passwordEncoder() {
    return PasswordEncoderFactories.createDelegatingPasswordEncoder();
  }

  /**
   * CORS for the OIDC endpoints called cross-origin by the dev SPA ({@code ng serve}); production
   * is same-origin behind the TLS proxy, so the origin list is empty there.
   */
  @Bean
  CorsConfigurationSource corsConfigurationSource(AppSecurityProperties properties) {
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    if (!properties.allowedOrigins().isEmpty()) {
      CorsConfiguration config = new CorsConfiguration();
      config.setAllowedOrigins(properties.allowedOrigins());
      config.setAllowedMethods(List.of("GET", "POST", "OPTIONS"));
      config.setAllowedHeaders(List.of("*"));
      source.registerCorsConfiguration("/oauth2/**", config);
      source.registerCorsConfiguration("/.well-known/**", config);
      source.registerCorsConfiguration("/userinfo", config);
      source.registerCorsConfiguration("/connect/**", config);
      source.registerCorsConfiguration("/api/**", config);
    }
    return source;
  }
}
