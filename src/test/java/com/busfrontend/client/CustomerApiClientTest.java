package com.busfrontend.client;

import com.busfrontend.dto.CustomerRequestDTO;
import com.busfrontend.dto.CustomerResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class CustomerApiClientTest {

    private RestTemplate restTemplate;
    private MockRestServiceServer server;
    private CustomerApiClient client;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        server = MockRestServiceServer.createServer(restTemplate);
        client = new CustomerApiClient(restTemplate);
        ReflectionTestUtils.setField(client, "baseUrl", "http://localhost:8080");
    }

    @Test
    void getAllReturnsCustomers() {
        String body = "[{\"id\":1,\"name\":\"Alice\",\"email\":\"a@x.com\",\"phone\":\"1111111111\"}," +
                "{\"id\":2,\"name\":\"Bob\",\"email\":\"b@x.com\",\"phone\":\"2222222222\"}]";
        server.expect(requestTo("http://localhost:8080/api/customers"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        var list = client.getAll();

        assertThat(list).hasSize(2);
        assertThat(list.get(0).getName()).isEqualTo("Alice");
        server.verify();
    }

    @Test
    void getByIdReturnsCustomer() {
        String body = "{\"id\":5,\"name\":\"Charlie\",\"email\":\"c@x.com\",\"phone\":\"3333333333\",\"city\":\"Pune\"}";
        server.expect(requestTo("http://localhost:8080/api/customers/5"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        CustomerResponseDTO c = client.getById(5);

        assertThat(c.getId()).isEqualTo(5);
        assertThat(c.getCity()).isEqualTo("Pune");
        server.verify();
    }

    @Test
    void createPostsCustomerAndReturnsCreated() {
        CustomerRequestDTO req = new CustomerRequestDTO();
        req.setName("Dave");
        req.setEmail("d@x.com");
        req.setPhone("4444444444");
        req.setAddressId(1);

        String body = "{\"id\":99,\"name\":\"Dave\",\"email\":\"d@x.com\",\"phone\":\"4444444444\"}";
        server.expect(requestTo("http://localhost:8080/api/customers"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        CustomerResponseDTO created = client.create(req);

        assertThat(created.getId()).isEqualTo(99);
        server.verify();
    }

    @Test
    void updatePutsCustomerAndReturnsUpdated() {
        CustomerRequestDTO req = new CustomerRequestDTO();
        req.setName("Updated");
        req.setEmail("u@x.com");
        req.setPhone("5555555555");
        req.setAddressId(1);

        String body = "{\"id\":3,\"name\":\"Updated\"}";
        server.expect(requestTo("http://localhost:8080/api/customers/3"))
                .andExpect(method(HttpMethod.PUT))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        CustomerResponseDTO updated = client.update(3, req);

        assertThat(updated.getName()).isEqualTo("Updated");
        server.verify();
    }

    @Test
    void deleteIssuesDeleteRequest() {
        server.expect(requestTo("http://localhost:8080/api/customers/9"))
                .andExpect(method(HttpMethod.DELETE))
                .andRespond(withSuccess());

        client.delete(9);

        server.verify();
    }
}
