package com.busfrontend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class RestClientConfig {

    @Value("${backend.connect-timeout-ms:3000}")
    private long connectTimeoutMs;

    @Value("${backend.read-timeout-ms:15000}")
    private long readTimeoutMs;

    @Value("${backend.username:}")
    private String backendUsername;

    @Value("${backend.password:}")
    private String backendPassword;

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        RestTemplateBuilder b = builder
                .setConnectTimeout(Duration.ofMillis(connectTimeoutMs))
                .setReadTimeout(Duration.ofMillis(readTimeoutMs));
        if (!backendUsername.isBlank() && !backendPassword.isBlank()) {
            b = b.basicAuthentication(backendUsername, backendPassword);
        }
        return b.build();
    }
}
