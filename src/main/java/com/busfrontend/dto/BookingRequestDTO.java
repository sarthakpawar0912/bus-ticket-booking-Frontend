package com.busfrontend.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingRequestDTO {

    @NotNull(message = "Trip ID cannot be null")
    private Integer tripId;

    @NotEmpty(message = "Seat numbers cannot be empty")
    private List<Integer> seatNumbers;

    @NotNull(message = "Customer ID cannot be null")
    private Integer customerId;
}
