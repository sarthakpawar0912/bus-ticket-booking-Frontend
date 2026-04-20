package com.busfrontend.client;

import com.busfrontend.dto.BusRequestDTO;
import com.busfrontend.dto.BusResponseDTO;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Component
public class BusApiClient extends AbstractApiClient {

    public BusApiClient(RestTemplate restTemplate) { super(restTemplate); }

    public List<BusResponseDTO> getAll() {
        return getList("/api/buses", new ParameterizedTypeReference<List<BusResponseDTO>>() {});
    }

    public BusResponseDTO getById(Integer id) { return get("/api/buses/" + id, BusResponseDTO.class); }

    public List<BusResponseDTO> getByOffice(Integer officeId) {
        return getList("/api/buses/office/" + officeId, new ParameterizedTypeReference<List<BusResponseDTO>>() {});
    }

    public BusResponseDTO create(BusRequestDTO dto) { return post("/api/buses", dto, BusResponseDTO.class); }

    public BusResponseDTO update(Integer id, BusRequestDTO dto) { return put("/api/buses/" + id, dto, BusResponseDTO.class); }
}
