package com.busfrontend.client;

import com.busfrontend.dto.OfficeRequestDTO;
import com.busfrontend.dto.OfficeResponseDTO;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Component
public class AgencyOfficeApiClient extends AbstractApiClient {

    public AgencyOfficeApiClient(RestTemplate restTemplate) { super(restTemplate); }

    public List<OfficeResponseDTO> getAll() {
        return getList("/api/offices", new ParameterizedTypeReference<List<OfficeResponseDTO>>() {});
    }

    public OfficeResponseDTO getById(Integer id) { return get("/api/offices/" + id, OfficeResponseDTO.class); }

    public List<OfficeResponseDTO> getByAgency(Integer agencyId) {
        return getList("/api/offices/agency/" + agencyId,
                new ParameterizedTypeReference<List<OfficeResponseDTO>>() {});
    }

    public OfficeResponseDTO create(OfficeRequestDTO dto) { return post("/api/offices", dto, OfficeResponseDTO.class); }

    public OfficeResponseDTO update(Integer id, OfficeRequestDTO dto) { return put("/api/offices/" + id, dto, OfficeResponseDTO.class); }
}
