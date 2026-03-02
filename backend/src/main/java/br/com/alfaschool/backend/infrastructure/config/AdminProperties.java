package br.com.alfaschool.backend.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.admin")
public record AdminProperties(
        String email,
        String name,
        String password
) {
}
