package com.busfrontend.client;

import com.busfrontend.dto.CustomerRequestDTO;
import com.busfrontend.dto.CustomerResponseDTO;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Component
public class CustomerApiClient extends AbstractApiClient {

    public CustomerApiClient(RestTemplate restTemplate) { super(restTemplate); }

    public List<CustomerResponseDTO> getAll() {
        return getList("/api/customers", new ParameterizedTypeReference<List<CustomerResponseDTO>>() {});
    }

    public CustomerResponseDTO getById(Integer id) { return get("/api/customers/" + id, CustomerResponseDTO.class); }

    public CustomerResponseDTO create(CustomerRequestDTO dto) { return post("/api/customers", dto, CustomerResponseDTO.class); }

    public CustomerResponseDTO update(Integer id, CustomerRequestDTO dto) { return put("/api/customers/" + id, dto, CustomerResponseDTO.class); }

    public void delete(Integer id) { delete("/api/customers/" + id); }
}
