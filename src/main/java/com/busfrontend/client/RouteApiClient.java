package com.busfrontend.client;

import com.busfrontend.dto.RouteDTO;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Component
public class RouteApiClient extends AbstractApiClient {

    public RouteApiClient(RestTemplate restTemplate) { super(restTemplate); }

    public List<RouteDTO> getAll() {
        return getList("/api/routes", new ParameterizedTypeReference<List<RouteDTO>>() {});
    }

    public RouteDTO getById(Integer id) { return get("/api/routes/" + id, RouteDTO.class); }

    public RouteDTO create(RouteDTO dto) { return post("/api/routes", dto, RouteDTO.class); }

    public RouteDTO update(Integer id, RouteDTO dto) { return put("/api/routes/" + id, dto, RouteDTO.class); }

    public void delete(Integer id) { delete("/api/routes/" + id); }
}
