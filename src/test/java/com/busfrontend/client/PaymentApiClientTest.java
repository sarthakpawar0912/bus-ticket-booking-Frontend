package com.busfrontend.client;

import com.busfrontend.dto.PaymentRequestDTO;
import com.busfrontend.dto.PaymentResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class PaymentApiClientTest {

    private RestTemplate restTemplate;
    private MockRestServiceServer server;
    private PaymentApiClient client;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        server = MockRestServiceServer.createServer(restTemplate);
        client = new PaymentApiClient(restTemplate);
        ReflectionTestUtils.setField(client, "baseUrl", "http://localhost:8080");
    }

    @Test
    void processPaymentPostsRequestAndReturnsResponse() {
        PaymentRequestDTO req = new PaymentRequestDTO(1, 2, new BigDecimal("500.00"));
        String body = "{\"paymentId\":100,\"bookingId\":1,\"customerId\":2,\"amount\":500.00,\"paymentStatus\":\"Success\"}";
        server.expect(requestTo("http://localhost:8080/api/payments"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        PaymentResponseDTO response = client.processPayment(req);

        assertThat(response).isNotNull();
        assertThat(response.getPaymentId()).isEqualTo(100);
        assertThat(response.getAmount()).isEqualByComparingTo("500.00");
        server.verify();
    }

    @Test
    void getByBookingIdFetchesByPath() {
        String body = "{\"paymentId\":50,\"bookingId\":10,\"customerId\":2,\"amount\":300.00,\"paymentStatus\":\"Success\"}";
        server.expect(requestTo("http://localhost:8080/api/payments/booking/10"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        PaymentResponseDTO response = client.getByBookingId(10);

        assertThat(response.getBookingId()).isEqualTo(10);
        server.verify();
    }

    @Test
    void getByIdFetchesPayment() {
        String body = "{\"paymentId\":7,\"bookingId\":1,\"customerId\":2,\"amount\":200.00,\"paymentStatus\":\"Success\"}";
        server.expect(requestTo("http://localhost:8080/api/payments/7"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        PaymentResponseDTO response = client.getById(7);

        assertThat(response.getPaymentId()).isEqualTo(7);
        server.verify();
    }

    @Test
    void getAllReturnsPayments() {
        String body = "[{\"paymentId\":1,\"amount\":100.00,\"paymentStatus\":\"Success\"}," +
                "{\"paymentId\":2,\"amount\":200.00,\"paymentStatus\":\"Failed\"}]";
        server.expect(requestTo("http://localhost:8080/api/payments"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        var list = client.getAll();

        assertThat(list).hasSize(2);
        assertThat(list.get(0).getPaymentId()).isEqualTo(1);
        server.verify();
    }

    @Test
    void downloadTicketByPaymentIdReturnsBytes() {
        byte[] pdf = new byte[]{'%', 'P', 'D', 'F', '-', '1'};
        server.expect(requestTo("http://localhost:8080/api/payments/55/ticket"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(pdf, MediaType.APPLICATION_PDF));

        byte[] result = client.downloadTicketByPaymentId(55);

        assertThat(result).containsExactly(pdf);
        server.verify();
    }

    @Test
    void getByCustomerIdReturnsPaymentList() {
        String body = "[{\"paymentId\":1,\"customerId\":5,\"amount\":100.00,\"paymentStatus\":\"Success\"}]";
        server.expect(requestTo("http://localhost:8080/api/payments/customer/5"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        var list = client.getByCustomerId(5);

        assertThat(list).hasSize(1);
        assertThat(list.get(0).getCustomerId()).isEqualTo(5);
        server.verify();
    }
}
