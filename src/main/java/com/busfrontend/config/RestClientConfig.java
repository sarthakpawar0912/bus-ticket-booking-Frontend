package com.busfrontend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.HttpURLConnection;
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
        // Don't follow redirects — if the backend redirects to /login, we want to
        // see the 302 (treat it as auth failure), NOT silently fetch the HTML
        // login page and present it to the user as if it were API data.
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory() {
            @Override
            protected void prepareConnection(HttpURLConnection connection, String httpMethod) throws IOException {
                super.prepareConnection(connection, httpMethod);
                connection.setInstanceFollowRedirects(false);
            }
        };

        RestTemplateBuilder b = builder
                .requestFactory(() -> factory)
                .setConnectTimeout(Duration.ofMillis(connectTimeoutMs))
                .setReadTimeout(Duration.ofMillis(readTimeoutMs));
        if (!backendUsername.isBlank() && !backendPassword.isBlank()) {
            b = b.basicAuthentication(backendUsername, backendPassword);
        }
        return b.build();
    }
}
