package com.chatrealtime.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthCookieStartupValidatorTest {

    @Test
    void run_whenStrictAndSecure_shouldPass() {
        AuthCookieStartupValidator validator = new AuthCookieStartupValidator(
                new AuthCookieProperties("refreshToken", "/api/auth", "Strict", true)
        );

        assertThatCode(() -> validator.run(null)).doesNotThrowAnyException();
    }

    @Test
    void run_whenSameSiteIsNotStrict_shouldFailFast() {
        AuthCookieStartupValidator validator = new AuthCookieStartupValidator(
                new AuthCookieProperties("refreshToken", "/api/auth", "None", true)
        );

        assertThatThrownBy(() -> validator.run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("same-site");
    }

    @Test
    void run_whenCookieIsNotSecure_shouldFailFast() {
        AuthCookieStartupValidator validator = new AuthCookieStartupValidator(
                new AuthCookieProperties("refreshToken", "/api/auth", "Strict", false)
        );

        assertThatThrownBy(() -> validator.run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("secure");
    }
}
