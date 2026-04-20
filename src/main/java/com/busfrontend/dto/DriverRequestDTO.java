package com.busfrontend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class DriverRequestDTO {

    @NotBlank(message = "License number is required")
    private String licenseNumber;

    @NotBlank(message = "Driver name is required")
    private String name;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\d{10}$", message = "Phone must be exactly 10 digits")
    private String phone;

    @NotNull(message = "Office ID is required to assign this driver")
    private Integer officeId;

    @NotNull(message = "Address ID is required")
    private Integer addressId;
}