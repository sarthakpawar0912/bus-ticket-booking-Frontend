package com.busfrontend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TripDTO {

    private Integer tripId;

    private Integer routeId;

    private Integer busId;

    private Integer boardingAddressId;

    private Integer droppingAddressId;

    private LocalDateTime departureTime;

    private LocalDateTime arrivalTime;

    private Integer driver1Id;

    private Integer driver2Id;

    private Integer availableSeats;

    private BigDecimal fare;

    private LocalDateTime tripDate;

    // Display fields
    private String fromCity;

    private String toCity;

    private String busType;

    private String registrationNumber;

    private String boardingAddress;

    private String droppingAddress;
}
