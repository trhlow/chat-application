package com.chatrealtime.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtSecretValidatorTest {

    @Test
    void relaxedProfile_acceptsWeakSecret() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("local");
        JwtProperties props = new JwtProperties("change-this-dev-secret-change-this-dev-secret", 900000, 604800000);
        assertThatCode(() -> JwtSecretValidator.validate(env, props)).doesNotThrowAnyException();
    }

    @Test
    void staging_blankSecret_throws() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("staging");
        JwtProperties props = new JwtProperties("   ", 900000, 604800000);
        assertThatThrownBy(() -> JwtSecretValidator.validate(env, props))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("APP_JWT_SECRET");
    }

    @Test
    void staging_shortSecret_throws() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");
        JwtProperties props = new JwtProperties("short", 900000, 604800000);
        assertThatThrownBy(() -> JwtSecretValidator.validate(env, props))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32");
    }

    @Test
    void staging_changeThis_throws() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");
        JwtProperties props = new JwtProperties("change-this-is-long-enough-xxxxxxxxxxxxxx", 900000, 604800000);
        assertThatThrownBy(() -> JwtSecretValidator.validate(env, props))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("weak");
    }
}
