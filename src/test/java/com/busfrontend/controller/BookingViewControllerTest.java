package com.busfrontend.controller;

import com.busfrontend.client.BackendException;
import com.busfrontend.client.BookingApiClient;
import com.busfrontend.client.BusApiClient;
import com.busfrontend.client.CustomerApiClient;
import com.busfrontend.client.TripApiClient;
import com.busfrontend.dto.BookingResponseDTO;
import com.busfrontend.dto.BusResponseDTO;
import com.busfrontend.dto.TripDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(BookingViewController.class)
class BookingViewControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private BookingApiClient bookingApiClient;
    @MockBean private TripApiClient tripApiClient;
    @MockBean private CustomerApiClient customerApiClient;
    @MockBean private BusApiClient busApiClient;

    @Test
    void showSeatSelectionRendersSelectSeatView() throws Exception {
        TripDTO trip = TripDTO.builder().tripId(1).busId(10).fromCity("A").toCity("B")
                .fare(new BigDecimal("100")).availableSeats(30).build();
        BusResponseDTO bus = BusResponseDTO.builder().busId(10).capacity(30).build();

        when(tripApiClient.getById(1)).thenReturn(trip);
        when(busApiClient.getById(10)).thenReturn(bus);
        when(bookingApiClient.getByTrip(1)).thenReturn(List.of());
        when(customerApiClient.getAll()).thenReturn(List.of());

        mockMvc.perform(get("/view/bookings/trip/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("booking/select-seat"))
                .andExpect(model().attributeExists("trip", "allSeats", "bookedSeats", "customers"));
    }

    @Test
    void bookSeatsRedirectsToConfirmationOnSuccess() throws Exception {
        BookingResponseDTO response = BookingResponseDTO.builder()
                .message("Booked")
                .bookingIds(List.of(501, 502))
                .totalFare(new BigDecimal("200"))
                .customerId(5)
                .build();
        when(bookingApiClient.createBooking(any())).thenReturn(response);
        when(customerApiClient.getById(5)).thenReturn(null);
        when(tripApiClient.getById(1)).thenReturn(null);

        mockMvc.perform(post("/view/bookings/book")
                        .param("tripId", "1")
                        .param("seatNumbers", "1", "2")
                        .param("customerId", "5"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/view/bookings/confirmation/501"))
                .andExpect(flash().attributeExists("booking", "seatNumbers"));
    }

    @Test
    void bookSeatsFlashesErrorAndRedirectsBackOnBackendFailure() throws Exception {
        when(bookingApiClient.createBooking(any()))
                .thenThrow(new BackendException(400, "Seats already booked"));

        mockMvc.perform(post("/view/bookings/book")
                        .param("tripId", "1")
                        .param("seatNumbers", "1")
                        .param("customerId", "5"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/view/bookings/trip/1"))
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    void listAvailableTripsFiltersOutFullTrips() throws Exception {
        TripDTO available = TripDTO.builder().tripId(1).availableSeats(10).build();
        TripDTO full = TripDTO.builder().tripId(2).availableSeats(0).build();
        when(tripApiClient.getAllTrips()).thenReturn(List.of(available, full));

        mockMvc.perform(get("/view/bookings"))
                .andExpect(status().isOk())
                .andExpect(view().name("booking/trips"))
                .andExpect(model().attributeExists("trips"));
    }
}
