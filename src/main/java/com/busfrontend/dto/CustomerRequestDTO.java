package com.busfrontend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class CustomerRequestDTO {

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Invalid Email")
    private String email;

    @Pattern(regexp ="^\\d{10}$" ,message = "Phone must be 10 digit")
    private String phone;

    @NotNull
    private Integer addressId;
}
