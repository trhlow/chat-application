package com.chatrealtime.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Profile("prod")
@Order(0)
@RequiredArgsConstructor
public class AuthCookieStartupValidator implements ApplicationRunner {

    private final AuthCookieProperties authCookieProperties;

    @Override
    public void run(ApplicationArguments args) {
        if (!"Strict".equalsIgnoreCase(authCookieProperties.sameSite())) {
            throw new IllegalStateException(
                    "app.auth.refresh-cookie.same-site must be Strict in prod while CSRF is disabled"
            );
        }
        if (!authCookieProperties.secure()) {
            throw new IllegalStateException("app.auth.refresh-cookie.secure must be true in prod");
        }
    }
}
