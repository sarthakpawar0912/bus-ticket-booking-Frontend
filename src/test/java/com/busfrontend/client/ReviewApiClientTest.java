package com.busfrontend.client;

import com.busfrontend.dto.ReviewDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ReviewApiClientTest {

    private RestTemplate restTemplate;
    private MockRestServiceServer server;
    private ReviewApiClient client;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        server = MockRestServiceServer.createServer(restTemplate);
        client = new ReviewApiClient(restTemplate);
        ReflectionTestUtils.setField(client, "baseUrl", "http://localhost:8080");
    }

    @Test
    void createPostsReviewAndReturnsMap() {
        ReviewDTO dto = ReviewDTO.builder()
                .customerId(1).tripId(2).rating(5).comment("Great trip").build();
        String body = "{\"reviewId\":10,\"customerId\":1,\"tripId\":2,\"rating\":5,\"comment\":\"Great trip\"}";
        server.expect(requestTo("http://localhost:8080/api/reviews"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        Map<String, Object> result = client.create(dto);

        assertThat(result).isNotNull();
        assertThat(result.get("reviewId")).isEqualTo(10);
        server.verify();
    }

    @Test
    void getByTripReturnsListOfReviews() {
        String body = "[{\"reviewId\":1,\"tripId\":7,\"rating\":5}," +
                "{\"reviewId\":2,\"tripId\":7,\"rating\":4}]";
        server.expect(requestTo("http://localhost:8080/api/reviews/trip/7"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        var list = client.getByTrip(7);

        assertThat(list).hasSize(2);
        assertThat(list.get(0).get("rating")).isEqualTo(5);
        server.verify();
    }

    @Test
    void getAllReturnsAllReviews() {
        String body = "[{\"reviewId\":1,\"rating\":5},{\"reviewId\":2,\"rating\":3}]";
        server.expect(requestTo("http://localhost:8080/api/reviews"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        var list = client.getAll();

        assertThat(list).hasSize(2);
        server.verify();
    }

    @Test
    void getByCustomerReturnsReviews() {
        String body = "[{\"reviewId\":1,\"customerId\":3,\"rating\":4}]";
        server.expect(requestTo("http://localhost:8080/api/reviews/customer/3"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        var list = client.getByCustomer(3);

        assertThat(list).hasSize(1);
        assertThat(list.get(0).get("customerId")).isEqualTo(3);
        server.verify();
    }

    @Test
    void getAllReturnsEmptyListWhenNone() {
        server.expect(requestTo("http://localhost:8080/api/reviews"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        var list = client.getAll();

        assertThat(list).isEmpty();
        server.verify();
    }
}
