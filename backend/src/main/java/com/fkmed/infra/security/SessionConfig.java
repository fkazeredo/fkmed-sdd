package com.fkmed.infra.security;

import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.security.web.authentication.SpringSessionRememberMeServices;
import org.springframework.session.web.http.DefaultCookieSerializer;

/**
 * Spring Session cookie + remember-me wiring for the two BR12 idle windows (ADR-0005). The default
 * 30-minute timeout ({@code server.servlet.session.timeout}) governs the "Manter conectado"
 * unchecked case with a session cookie that dies on browser close; when the login form submits the
 * {@code remember-me} parameter, {@link SpringSessionRememberMeServices} raises that session's
 * max-inactive-interval to 7 days and the {@link DefaultCookieSerializer} — told the remember-me
 * request attribute — writes a persistent cookie ({@code Max-Age}) that survives a browser restart.
 * The 7-day idle window is enforced server-side by the session's max-inactive-interval, not by the
 * cookie's (deliberately long) lifetime.
 */
@Configuration
public class SessionConfig {

  static final int REMEMBER_ME_VALIDITY_SECONDS = (int) Duration.ofDays(7).toSeconds();

  @Bean
  SpringSessionRememberMeServices rememberMeServices() {
    SpringSessionRememberMeServices services = new SpringSessionRememberMeServices();
    services.setAlwaysRemember(false);
    services.setValiditySeconds(REMEMBER_ME_VALIDITY_SECONDS);
    return services;
  }

  @Bean
  DefaultCookieSerializer cookieSerializer() {
    DefaultCookieSerializer serializer = new DefaultCookieSerializer();
    serializer.setRememberMeRequestAttribute(
        SpringSessionRememberMeServices.REMEMBER_ME_LOGIN_ATTR);
    serializer.setSameSite("Lax");
    return serializer;
  }
}
