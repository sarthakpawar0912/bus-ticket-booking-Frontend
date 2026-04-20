package com.busfrontend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class OfficeRequestDTO {

    @NotNull(message = "Agency ID is required to link this office to a parent company")
    private Integer agencyId;

    @NotBlank(message = "Office email is required")
    @Email(message = "Invalid email format")
    private String officeMail;

    @NotBlank(message = "Office contact person name is required")
    private String officeContactPersonName;

    @NotBlank(message = "Office contact number is required")
    @Pattern(regexp = "^\\d{10}$", message = "Phone must be exactly 10 digits")
    private String officeContactNumber;

    @NotNull(message = "Office Address ID is required")
    private Integer officeAddressId;
}