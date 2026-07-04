package com.fkmed.infra.observability;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/** SPEC-0001 §Observability: access log excludes health checks. */
class AccessLogFilterTest {

  private final AccessLogFilter filter = new AccessLogFilter();

  @Test
  void healthAndActuator_areExcludedFromTheAccessLog() {
    assertThat(filter.shouldNotFilter(new MockHttpServletRequest("GET", "/api/system/health")))
        .isTrue();
    assertThat(filter.shouldNotFilter(new MockHttpServletRequest("GET", "/actuator/health")))
        .isTrue();
    assertThat(filter.shouldNotFilter(new MockHttpServletRequest("GET", "/api/plan/my-plan")))
        .isFalse();
  }

  @Test
  void filter_letsTheRequestThrough() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/plan/my-plan");
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain chain = new MockFilterChain();

    filter.doFilter(request, response, chain);

    assertThat(chain.getRequest()).isSameAs(request);
  }
}
