package com.busfrontend.client;

import com.busfrontend.dto.TripDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class TripApiClientTest {

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
    void getAllTripsReturnsList() {
        String body = "[{\"tripId\":1,\"fromCity\":\"Mumbai\",\"toCity\":\"Pune\",\"fare\":500,\"availableSeats\":30}," +
                "{\"tripId\":2,\"fromCity\":\"Pune\",\"toCity\":\"Nashik\",\"fare\":400,\"availableSeats\":25}]";
        server.expect(requestTo("http://localhost:8080/api/trips"))
                .andExpect(method(org.springframework.http.HttpMethod.GET))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        List<TripDTO> trips = client.getAllTrips();

        assertThat(trips).hasSize(2);
        assertThat(trips.get(0).getTripId()).isEqualTo(1);
        assertThat(trips.get(0).getFromCity()).isEqualTo("Mumbai");
        assertThat(trips.get(1).getToCity()).isEqualTo("Nashik");
        server.verify();
    }

    @Test
    void getByIdReturnsSingleTrip() {
        String body = "{\"tripId\":7,\"fromCity\":\"Delhi\",\"toCity\":\"Agra\",\"fare\":350}";
        server.expect(requestTo("http://localhost:8080/api/trips/7"))
                .andExpect(method(org.springframework.http.HttpMethod.GET))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        TripDTO trip = client.getById(7);

        assertThat(trip).isNotNull();
        assertThat(trip.getTripId()).isEqualTo(7);
        assertThat(trip.getFromCity()).isEqualTo("Delhi");
        server.verify();
    }

    @Test
    void getByIdThrowsBackendExceptionOn404() {
        server.expect(requestTo("http://localhost:8080/api/trips/999"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"message\":\"Trip not found\"}"));

        assertThatThrownBy(() -> client.getById(999))
                .isInstanceOf(BackendException.class)
                .hasMessage("Trip not found");

        server.verify();
    }

    @Test
    void searchAddsQueryParams() {
        String body = "[{\"tripId\":1,\"fromCity\":\"Mumbai\",\"toCity\":\"Pune\"}]";
        server.expect(requestTo("http://localhost:8080/api/trips/search?from=Mumbai&to=Pune"))
                .andExpect(method(org.springframework.http.HttpMethod.GET))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        List<TripDTO> results = client.search("Mumbai", "Pune");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getFromCity()).isEqualTo("Mumbai");
        server.verify();
    }

    @Test
    void createPostsTripAndReturnsCreated() {
        TripDTO req = TripDTO.builder()
                .routeId(1).busId(2).boardingAddressId(3).droppingAddressId(4)
                .fare(new BigDecimal("500"))
                .build();
        String body = "{\"tripId\":10,\"routeId\":1,\"busId\":2}";
        server.expect(requestTo("http://localhost:8080/api/trips"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        TripDTO created = client.create(req);

        assertThat(created.getTripId()).isEqualTo(10);
        server.verify();
    }

    @Test
    void updatePutsTripAndReturnsUpdated() {
        TripDTO req = TripDTO.builder().tripId(5).routeId(1).build();
        String body = "{\"tripId\":5,\"routeId\":1}";
        server.expect(requestTo("http://localhost:8080/api/trips/5"))
                .andExpect(method(org.springframework.http.HttpMethod.PUT))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        TripDTO updated = client.update(5, req);

        assertThat(updated.getTripId()).isEqualTo(5);
        server.verify();
    }
}
