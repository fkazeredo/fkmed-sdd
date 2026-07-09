package com.fkmed.infra.observability;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
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

  @Test
  void filter_reusesSafeInboundCorrelationId_andEchoesItToTheClient() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/plan/my-plan");
    request.addHeader(AccessLogFilter.CORRELATION_ID_HEADER, "qa-run-20260708");
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain chain =
        (servletRequest, servletResponse) ->
            assertThat(MDC.get(AccessLogFilter.CORRELATION_ID_MDC_KEY))
                .isEqualTo("qa-run-20260708");

    filter.doFilter(request, response, chain);

    assertThat(response.getHeader(AccessLogFilter.CORRELATION_ID_HEADER))
        .isEqualTo("qa-run-20260708");
    assertThat(MDC.get(AccessLogFilter.CORRELATION_ID_MDC_KEY)).isNull();
  }

  @Test
  void filter_rejectsUnsafeCorrelationIdHeader_andGeneratesANewOne() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/plan/my-plan");
    request.addHeader(AccessLogFilter.CORRELATION_ID_HEADER, "bad value with spaces");
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain chain = new MockFilterChain();

    filter.doFilter(request, response, chain);

    assertThat(response.getHeader(AccessLogFilter.CORRELATION_ID_HEADER))
        .isNotEqualTo("bad value with spaces")
        .matches("[0-9a-f-]{36}");
  }
}
