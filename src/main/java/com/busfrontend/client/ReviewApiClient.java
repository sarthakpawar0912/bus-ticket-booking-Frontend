package com.busfrontend.client;

import com.busfrontend.dto.ReviewDTO;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Component
public class ReviewApiClient extends AbstractApiClient {

    public ReviewApiClient(RestTemplate restTemplate) { super(restTemplate); }

    @SuppressWarnings("unchecked")
    public Map<String, Object> create(ReviewDTO dto) { return post("/api/reviews", dto, Map.class); }

    public List<Map<String, Object>> getAll() {
        return getList("/api/reviews", new ParameterizedTypeReference<List<Map<String, Object>>>() {});
    }

    public List<Map<String, Object>> getByTrip(Integer tripId) {
        return getList("/api/reviews/trip/" + tripId, new ParameterizedTypeReference<List<Map<String, Object>>>() {});
    }

    public List<Map<String, Object>> getByCustomer(Integer customerId) {
        return getList("/api/reviews/customer/" + customerId,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {});
    }
}
