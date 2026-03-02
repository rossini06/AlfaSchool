package br.com.alfaschool.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security")
public record JwtProperties(
        String jwtSecret,
        long jwtExpirationMinutes
) {
}
