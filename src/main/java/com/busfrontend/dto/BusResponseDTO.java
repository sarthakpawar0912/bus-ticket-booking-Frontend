package com.busfrontend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder // ✅ Makes converting from your Bus Entity to this DTO effortless in the Service
public class BusResponseDTO {

    private Integer busId;

    // We send back the Office ID instead of the whole Office object to keep the JSON light and fast
    private Integer officeId;

    private String registrationNumber;
    private Integer capacity;
    private String type;

}