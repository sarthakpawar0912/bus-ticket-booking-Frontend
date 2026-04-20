package com.busfrontend.client;

import com.busfrontend.dto.BookingRequestDTO;
import com.busfrontend.dto.BookingResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class BookingApiClientTest {

    private RestTemplate restTemplate;
    private MockRestServiceServer server;
    private BookingApiClient client;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        server = MockRestServiceServer.createServer(restTemplate);
        client = new BookingApiClient(restTemplate);
        ReflectionTestUtils.setField(client, "baseUrl", "http://localhost:8080");
    }

    @Test
    void createBookingPostsRequestAndReturnsResponse() {
        BookingRequestDTO request = BookingRequestDTO.builder()
                .tripId(1).customerId(2).seatNumbers(List.of(5, 6)).build();
        String body = "{\"message\":\"Booking confirmed\",\"bookingIds\":[101,102],\"totalFare\":1000,\"customerId\":2}";
        server.expect(requestTo("http://localhost:8080/api/bookings"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        BookingResponseDTO response = client.createBooking(request);

        assertThat(response).isNotNull();
        assertThat(response.getBookingIds()).containsExactly(101, 102);
        assertThat(response.getCustomerId()).isEqualTo(2);
        server.verify();
    }

    @Test
    void getByIdReturnsBookingMap() {
        String body = "{\"bookingId\":101,\"tripId\":1,\"seatNumber\":5,\"status\":\"Booked\"}";
        server.expect(requestTo("http://localhost:8080/api/bookings/101"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        @SuppressWarnings("rawtypes")
        java.util.Map result = client.getById(101);

        assertThat(result).isNotNull();
        assertThat(result.get("seatNumber")).isEqualTo(5);
        server.verify();
    }

    @Test
    void getByTripReturnsListOfBookings() {
        String body = "[{\"bookingId\":1,\"seatNumber\":1,\"status\":\"Booked\"}," +
                "{\"bookingId\":2,\"seatNumber\":2,\"status\":\"Cancelled\"}]";
        server.expect(requestTo("http://localhost:8080/api/bookings/trip/7"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        var list = client.getByTrip(7);

        assertThat(list).hasSize(2);
        assertThat(list.get(0).get("status")).isEqualTo("Booked");
        server.verify();
    }

    @Test
    void downloadTicketReturnsBytes() {
        byte[] pdf = new byte[]{'%', 'P', 'D', 'F', '-'};
        server.expect(requestTo("http://localhost:8080/api/bookings/99/ticket"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(pdf, MediaType.APPLICATION_PDF));

        byte[] result = client.downloadTicket(99);

        assertThat(result).containsExactly(pdf);
        server.verify();
    }

    @Test
    void downloadGroupTicketBuildsCsvQueryParam() {
        byte[] pdf = new byte[]{'%', 'P', 'D', 'F'};
        server.expect(requestTo("http://localhost:8080/api/bookings/group-ticket?bookingIds=1,2,3"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(pdf, MediaType.APPLICATION_PDF));

        byte[] result = client.downloadGroupTicket(List.of(1, 2, 3));

        assertThat(result).containsExactly(pdf);
        server.verify();
    }

    @Test
    void createBookingThrowsOnBadRequestWithMessage() {
        BookingRequestDTO request = BookingRequestDTO.builder()
                .tripId(1).customerId(2).seatNumbers(List.of(5)).build();
        server.expect(requestTo("http://localhost:8080/api/bookings"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"message\":\"Seat already booked\"}"));

        assertThatThrownBy(() -> client.createBooking(request))
                .isInstanceOf(BackendException.class)
                .hasMessage("Seat already booked");

        server.verify();
    }
}
