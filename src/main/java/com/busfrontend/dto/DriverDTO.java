package com.busfrontend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DriverDTO {

    private Integer driverId;

    @NotBlank(message = "License number is required")
    private String licenseNumber;

    @NotBlank(message = "Name is required")
    private String name;

    @Pattern(regexp = "^\\d{10}$", message = "Phone must be 10 digits")
    private String phone;

    private Integer officeId;

    private Integer addressId;
}
