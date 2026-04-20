package com.busfrontend.client;

import com.busfrontend.dto.DriverRequestDTO;
import com.busfrontend.dto.DriverResponseDTO;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Component
public class DriverApiClient extends AbstractApiClient {

    public DriverApiClient(RestTemplate restTemplate) { super(restTemplate); }

    public List<DriverResponseDTO> getAll() {
        return getList("/api/drivers", new ParameterizedTypeReference<List<DriverResponseDTO>>() {});
    }

    public DriverResponseDTO getById(Integer id) { return get("/api/drivers/" + id, DriverResponseDTO.class); }

    public List<DriverResponseDTO> getByOffice(Integer officeId) {
        return getList("/api/drivers/office/" + officeId,
                new ParameterizedTypeReference<List<DriverResponseDTO>>() {});
    }

    public DriverResponseDTO create(DriverRequestDTO dto) { return post("/api/drivers", dto, DriverResponseDTO.class); }

    public DriverResponseDTO update(Integer id, DriverRequestDTO dto) { return put("/api/drivers/" + id, dto, DriverResponseDTO.class); }
}
