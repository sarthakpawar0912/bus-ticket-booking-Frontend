package com.busfrontend.client;

import com.busfrontend.dto.TripDTO;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@Component
public class TripApiClient extends AbstractApiClient {

    public TripApiClient(RestTemplate restTemplate) { super(restTemplate); }

    public List<TripDTO> getAllTrips() {
        return getList("/api/trips", new ParameterizedTypeReference<List<TripDTO>>() {});
    }

    public TripDTO getById(Integer id) {
        return get("/api/trips/" + id, TripDTO.class);
    }

    public TripDTO create(TripDTO dto) {
        return post("/api/trips", dto, TripDTO.class);
    }

    public TripDTO update(Integer id, TripDTO dto) {
        return put("/api/trips/" + id, dto, TripDTO.class);
    }

    public List<TripDTO> search(String from, String to) {
        String path = UriComponentsBuilder.fromPath("/api/trips/search")
                .queryParam("from", from).queryParam("to", to)
                .build().toUriString();
        return getList(path, new ParameterizedTypeReference<List<TripDTO>>() {});
    }
}
