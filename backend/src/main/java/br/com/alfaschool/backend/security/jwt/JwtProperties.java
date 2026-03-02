package br.com.alfaschool.backend.security.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security")
public record JwtProperties(
        String jwtSecret,
        long jwtExpirationMinutes,
        long refreshExpirationMinutes
) {
}
