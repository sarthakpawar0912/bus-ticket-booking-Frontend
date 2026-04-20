package com.busfrontend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BusRequestDTO {

    @NotNull(message = "Office ID is required to assign this bus")
    private Integer officeId;

    @NotBlank(message = "Registration number (License Plate) is required")
    private String registrationNumber;

    @NotNull(message = "Bus capacity is required")
    @Min(value = 10, message = "A bus must have a capacity of at least 10 seats")
    private Integer capacity;

    @NotBlank(message = "Bus type is required (e.g., AC Sleeper, Non-AC Seater)")
    private String type;
}