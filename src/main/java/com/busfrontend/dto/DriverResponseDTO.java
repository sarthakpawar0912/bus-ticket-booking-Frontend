package com.busfrontend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder // ✅ Crucial for easily mapping Entity -> DTO in your Service
public class DriverResponseDTO {

    private Integer driverId;
    private String licenseNumber;
    private String name;
    private String phone;

    // Returning IDs instead of full nested objects is a REST API best practice
    private Integer officeId;
    private Integer addressId;

}