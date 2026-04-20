package com.busfrontend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder // ✅ Builder makes it super easy to convert from Entity to DTO in the Service layer!
public class AgencyResponseDTO {

    private Integer agencyId;
    private String name;
    private String contactPersonName;
    private String email;
    private String phone;

}