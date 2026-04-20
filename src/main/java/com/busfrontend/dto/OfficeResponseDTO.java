package com.busfrontend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder // ✅ Makes mapping Entity -> DTO effortless in your Service
public class OfficeResponseDTO {

    private Integer officeId;

    // Returning IDs instead of full objects
    private Integer agencyId;
    private Integer officeAddressId;

    private String officeMail;
    private String officeContactPersonName;
    private String officeContactNumber;

}