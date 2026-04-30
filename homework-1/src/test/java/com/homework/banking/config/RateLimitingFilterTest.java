package com.homework.banking.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitingFilterTest {

    @Test
    void allowsFirst100RequestsFromSameIp() throws Exception {
        RateLimitingFilter filter = new RateLimitingFilter();

        for (int i = 0; i < 100; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest();
            req.setRemoteAddr("10.0.0.1");
            MockHttpServletResponse resp = new MockHttpServletResponse();
            filter.doFilter(req, resp, new MockFilterChain());
            assertThat(resp.getStatus())
                    .as("Request %d should not be rate-limited", i + 1)
                    .isNotEqualTo(429);
        }
    }

    @Test
    void blocks101stRequestFromSameIp() throws Exception {
        RateLimitingFilter filter = new RateLimitingFilter();

        for (int i = 0; i < 100; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest();
            req.setRemoteAddr("10.0.0.2");
            filter.doFilter(req, new MockHttpServletResponse(), new MockFilterChain());
        }

        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRemoteAddr("10.0.0.2");
        MockHttpServletResponse resp = new MockHttpServletResponse();
        filter.doFilter(req, resp, new MockFilterChain());

        assertThat(resp.getStatus()).isEqualTo(429);
        assertThat(resp.getContentAsString()).contains("Rate limit exceeded");
    }

    @Test
    void differentIpsHaveIndependentCounters() throws Exception {
        RateLimitingFilter filter = new RateLimitingFilter();

        // Exhaust the limit for IP A
        for (int i = 0; i < 100; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest();
            req.setRemoteAddr("192.168.1.1");
            filter.doFilter(req, new MockHttpServletResponse(), new MockFilterChain());
        }

        // IP B should still be allowed
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRemoteAddr("192.168.1.2");
        MockHttpServletResponse resp = new MockHttpServletResponse();
        filter.doFilter(req, resp, new MockFilterChain());

        assertThat(resp.getStatus()).isNotEqualTo(429);
    }
}
