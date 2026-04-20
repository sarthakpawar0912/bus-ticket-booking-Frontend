package com.busfrontend.controller;

import com.busfrontend.client.AddressApiClient;
import com.busfrontend.client.BusApiClient;
import com.busfrontend.client.DriverApiClient;
import com.busfrontend.client.RouteApiClient;
import com.busfrontend.client.TripApiClient;
import com.busfrontend.dto.TripDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(TripViewController.class)
class TripViewControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private TripApiClient tripApiClient;
    @MockBean private RouteApiClient routeApiClient;
    @MockBean private BusApiClient busApiClient;
    @MockBean private AddressApiClient addressApiClient;
    @MockBean private DriverApiClient driverApiClient;

    @Test
    void listTripsReturnsTripsViewWithTripsAttribute() throws Exception {
        TripDTO trip = TripDTO.builder().tripId(1).fromCity("Mumbai").toCity("Pune").build();
        when(tripApiClient.getAllTrips()).thenReturn(List.of(trip));

        mockMvc.perform(get("/view/trips"))
                .andExpect(status().isOk())
                .andExpect(view().name("trip/trips"))
                .andExpect(model().attributeExists("trips"));
    }

    @Test
    void searchTripsWithoutParamsReturnsSearchViewOnly() throws Exception {
        mockMvc.perform(get("/view/trips/search"))
                .andExpect(status().isOk())
                .andExpect(view().name("trip/search-trips"))
                .andExpect(model().attributeDoesNotExist("results"));
    }

    @Test
    void searchTripsWithFromAndToPopulatesResults() throws Exception {
        TripDTO trip = TripDTO.builder().tripId(2).fromCity("Mumbai").toCity("Pune").build();
        when(tripApiClient.search("Mumbai", "Pune")).thenReturn(List.of(trip));

        mockMvc.perform(get("/view/trips/search").param("from", "Mumbai").param("to", "Pune"))
                .andExpect(status().isOk())
                .andExpect(view().name("trip/search-trips"))
                .andExpect(model().attributeExists("results", "from", "to"));
    }
}
