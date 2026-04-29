package com.chatrealtime.config;

import com.chatrealtime.security.JwtProperties;
import com.chatrealtime.security.JwtSecretValidator;
import com.chatrealtime.security.JwtTokenService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class JwtBeansConfig {

    @Bean
    JwtTokenService jwtTokenService(JwtProperties jwtProperties, Environment environment) {
        JwtSecretValidator.validate(environment, jwtProperties);
        return new JwtTokenService(jwtProperties);
    }
}
