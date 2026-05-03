package com.chatrealtime.unit;

import com.chatrealtime.config.AppRateLimitProperties;
import com.chatrealtime.security.InMemoryRateLimitCounterStore;
import com.chatrealtime.security.RateLimitFilter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitFilterTest {
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void doFilterInternal_whenLoginLimitExceeded_shouldReturnTooManyRequests() throws Exception {
        AppRateLimitProperties properties = properties();
        properties.setLoginLimitPerMinute(2);
        RateLimitFilter filter = newFilter(properties);
        CountingFilterChain chain = new CountingFilterChain();

        MockHttpServletResponse first = perform(filter, chain, "POST", "/api/auth/login", "203.0.113.10");
        MockHttpServletResponse second = perform(filter, chain, "POST", "/api/auth/login", "203.0.113.10");
        MockHttpServletResponse third = perform(filter, chain, "POST", "/api/auth/login", "203.0.113.10");

        assertThat(first.getStatus()).isEqualTo(HttpStatus.OK.value());
        assertThat(second.getStatus()).isEqualTo(HttpStatus.OK.value());
        assertThat(third.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
        assertThat(third.getHeader(HttpHeaders.RETRY_AFTER)).isEqualTo("60");

        JsonNode body = objectMapper.readTree(third.getContentAsString());
        assertThat(body.get("error").asText()).isEqualTo("TOO_MANY_REQUESTS");
        assertThat(body.get("status").asInt()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
        assertThat(chain.invocations).isEqualTo(2);
    }

    @Test
    void doFilterInternal_whenPathIsNotRateLimited_shouldContinue() throws Exception {
        RateLimitFilter filter = newFilter(properties());
        CountingFilterChain chain = new CountingFilterChain();

        MockHttpServletResponse response = perform(filter, chain, "GET", "/api/rooms", "203.0.113.11");

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
        assertThat(chain.invocations).isEqualTo(1);
    }

    @Test
    void doFilterInternal_whenDisabled_shouldContinueWithoutLimiting() throws Exception {
        AppRateLimitProperties properties = properties();
        properties.setEnabled(false);
        properties.setRegisterLimitPerMinute(1);
        RateLimitFilter filter = newFilter(properties);
        CountingFilterChain chain = new CountingFilterChain();

        MockHttpServletResponse first = perform(filter, chain, "POST", "/api/auth/register", "203.0.113.12");
        MockHttpServletResponse second = perform(filter, chain, "POST", "/api/auth/register", "203.0.113.12");

        assertThat(first.getStatus()).isEqualTo(HttpStatus.OK.value());
        assertThat(second.getStatus()).isEqualTo(HttpStatus.OK.value());
        assertThat(chain.invocations).isEqualTo(2);
    }

    @Test
    void doFilterInternal_whenForwardedHeadersAreNotTrusted_shouldUseRemoteAddress() throws Exception {
        AppRateLimitProperties properties = properties();
        properties.setLoginLimitPerMinute(1);
        RateLimitFilter filter = newFilter(properties);
        CountingFilterChain chain = new CountingFilterChain();

        MockHttpServletResponse first = perform(
                filter,
                chain,
                "POST",
                "/api/auth/login",
                "203.0.113.20",
                "198.51.100.1"
        );
        MockHttpServletResponse second = perform(
                filter,
                chain,
                "POST",
                "/api/auth/login",
                "203.0.113.20",
                "198.51.100.2"
        );

        assertThat(first.getStatus()).isEqualTo(HttpStatus.OK.value());
        assertThat(second.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
    }

    @Test
    void doFilterInternal_whenForwardedHeadersAreTrusted_shouldUseForwardedClient() throws Exception {
        AppRateLimitProperties properties = properties();
        properties.setTrustForwardedHeaders(true);
        properties.setLoginLimitPerMinute(1);
        RateLimitFilter filter = newFilter(properties);
        CountingFilterChain chain = new CountingFilterChain();

        MockHttpServletResponse first = perform(
                filter,
                chain,
                "POST",
                "/api/auth/login",
                "10.0.0.10",
                "198.51.100.1"
        );
        MockHttpServletResponse second = perform(
                filter,
                chain,
                "POST",
                "/api/auth/login",
                "10.0.0.10",
                "198.51.100.2"
        );

        assertThat(first.getStatus()).isEqualTo(HttpStatus.OK.value());
        assertThat(second.getStatus()).isEqualTo(HttpStatus.OK.value());
    }

    private MockHttpServletResponse perform(
            RateLimitFilter filter,
            CountingFilterChain chain,
            String method,
            String uri,
            String remoteAddr
    ) throws ServletException, IOException {
        return perform(filter, chain, method, uri, remoteAddr, null);
    }

    private MockHttpServletResponse perform(
            RateLimitFilter filter,
            CountingFilterChain chain,
            String method,
            String uri,
            String remoteAddr,
            String forwardedFor
    ) throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        request.setRemoteAddr(remoteAddr);
        if (forwardedFor != null) {
            request.addHeader("X-Forwarded-For", forwardedFor);
        }
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, chain);
        return response;
    }

    private AppRateLimitProperties properties() {
        AppRateLimitProperties properties = new AppRateLimitProperties();
        properties.setEnabled(true);
        properties.setLoginLimitPerMinute(5);
        properties.setRegisterLimitPerMinute(3);
        properties.setRefreshLimitPerMinute(20);
        properties.setUploadLimitPerMinute(30);
        properties.setWebsocketLimitPerMinute(20);
        return properties;
    }

    private RateLimitFilter newFilter(AppRateLimitProperties properties) {
        return new RateLimitFilter(properties, objectMapper, new InMemoryRateLimitCounterStore());
    }

    private static class CountingFilterChain implements FilterChain {
        private int invocations;

        @Override
        public void doFilter(ServletRequest request, ServletResponse response) {
            invocations++;
        }
    }
}
