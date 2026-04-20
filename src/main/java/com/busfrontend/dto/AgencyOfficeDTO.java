package com.busfrontend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgencyOfficeDTO {

    private Integer officeId;

    @NotNull(message = "Agency ID is required")
    private Integer agencyId;

    @Email(message = "Invalid email format")
    private String officeMail;

    @NotBlank(message = "Office contact person name is required")
    private String officeContactPersonName;

    @Pattern(regexp = "^\\d{10}$", message = "Phone must be 10 digits")
    private String officeContactNumber;

    @NotNull(message = "Office address ID is required")
    private Integer officeAddressId;

    // Display fields
    private String agencyName;
    private String officeCity;
}
