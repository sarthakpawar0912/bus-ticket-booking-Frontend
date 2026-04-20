package com.busfrontend.client;

import com.busfrontend.dto.AgencyRequestDTO;
import com.busfrontend.dto.AgencyResponseDTO;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Component
public class AgencyApiClient extends AbstractApiClient {

    public AgencyApiClient(RestTemplate restTemplate) { super(restTemplate); }

    public List<AgencyResponseDTO> getAll() {
        return getList("/api/agencies", new ParameterizedTypeReference<List<AgencyResponseDTO>>() {});
    }

    public AgencyResponseDTO getById(Integer id) { return get("/api/agencies/" + id, AgencyResponseDTO.class); }

    public AgencyResponseDTO create(AgencyRequestDTO dto) { return post("/api/agencies", dto, AgencyResponseDTO.class); }

    public AgencyResponseDTO update(Integer id, AgencyRequestDTO dto) { return put("/api/agencies/" + id, dto, AgencyResponseDTO.class); }
}
