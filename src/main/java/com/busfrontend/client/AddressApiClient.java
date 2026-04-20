package com.busfrontend.client;

import com.busfrontend.dto.AddressDTO;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Component
public class AddressApiClient extends AbstractApiClient {

    public AddressApiClient(RestTemplate restTemplate) { super(restTemplate); }

    public List<AddressDTO> getAll() {
        return getList("/api/addresses", new ParameterizedTypeReference<List<AddressDTO>>() {});
    }

    public AddressDTO getById(Integer id) { return get("/api/addresses/" + id, AddressDTO.class); }

    public AddressDTO create(AddressDTO dto) { return post("/api/addresses", dto, AddressDTO.class); }

    public AddressDTO update(Integer id, AddressDTO dto) { return put("/api/addresses/" + id, dto, AddressDTO.class); }

    public void delete(Integer id) { delete("/api/addresses/" + id); }
}
