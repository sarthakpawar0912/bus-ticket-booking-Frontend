package com.busfrontend.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

/**
 * Verifies that {@link AbstractApiClient} converts HTTP errors into
 * {@link BackendException} carrying the status code and the parsed
 * "message" field from the backend error body. Uses TripApiClient as
 * the concrete subclass under test.
 */
class AbstractApiClientErrorTranslationTest {

    private RestTemplate restTemplate;
    private MockRestServiceServer server;
    private TripApiClient client;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        server = MockRestServiceServer.createServer(restTemplate);
        client = new TripApiClient(restTemplate);
        ReflectionTestUtils.setField(client, "baseUrl", "http://localhost:8080");
    }

    @Test
    void translates404IntoBackendExceptionWithStatusAndParsedMessage() {
        server.expect(requestTo("http://localhost:8080/api/trips/99"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"timestamp\":\"now\",\"status\":404,\"error\":\"Not Found\",\"message\":\"Trip not found\"}"));

        assertThatThrownBy(() -> client.getById(99))
                .isInstanceOf(BackendException.class)
                .hasMessage("Trip not found")
                .extracting("status").isEqualTo(404);

        server.verify();
    }

    @Test
    void translates500IntoBackendExceptionWithStatusAndParsedMessage() {
        server.expect(requestTo("http://localhost:8080/api/trips/1"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"status\":500,\"message\":\"DB down\"}"));

        assertThatThrownBy(() -> client.getById(1))
                .isInstanceOf(BackendException.class)
                .hasMessage("DB down")
                .extracting("status").isEqualTo(500);

        server.verify();
    }

    @Test
    void fallsBackToRawBodyWhenMessageFieldMissing() {
        server.expect(requestTo("http://localhost:8080/api/trips/1"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.TEXT_PLAIN)
                        .body("bad request raw"));

        assertThatThrownBy(() -> client.getById(1))
                .isInstanceOf(BackendException.class)
                .hasMessage("bad request raw")
                .extracting("status").isEqualTo(400);

        server.verify();
    }

    @Test
    void emptyResponseBodyFallsBackToOriginalExceptionMessage() {
        server.expect(requestTo("http://localhost:8080/api/trips/1"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThatThrownBy(() -> client.getById(1))
                .isInstanceOf(BackendException.class)
                .extracting("status").isEqualTo(503);

        server.verify();
    }

    @Test
    void parsesMessageWhenMultipleKeysInBody() {
        server.expect(requestTo("http://localhost:8080/api/trips/1"))
                .andRespond(withStatus(HttpStatus.CONFLICT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"timestamp\":\"2024\",\"path\":\"/x\",\"message\":\"Conflict happened\",\"status\":409}"));

        assertThatThrownBy(() -> client.getById(1))
                .isInstanceOf(BackendException.class)
                .hasMessage("Conflict happened")
                .extracting("status").isEqualTo(409);

        server.verify();
    }

    @Test
    void backendExceptionExposesStatusGetter() {
        BackendException ex = new BackendException(418, "teapot");
        assertThat(ex.getStatus()).isEqualTo(418);
        assertThat(ex.getMessage()).isEqualTo("teapot");
    }
}
