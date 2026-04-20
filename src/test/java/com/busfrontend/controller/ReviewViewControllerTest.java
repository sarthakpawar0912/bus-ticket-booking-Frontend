package com.busfrontend.controller;

import com.busfrontend.client.BackendException;
import com.busfrontend.client.CustomerApiClient;
import com.busfrontend.client.ReviewApiClient;
import com.busfrontend.client.TripApiClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

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

@WebMvcTest(ReviewViewController.class)
class ReviewViewControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private ReviewApiClient reviewApiClient;
    @MockBean private CustomerApiClient customerApiClient;
    @MockBean private TripApiClient tripApiClient;

    @Test
    void listReviewsRendersReviewsViewWithModel() throws Exception {
        when(reviewApiClient.getAll()).thenReturn(List.of());

        mockMvc.perform(get("/view/reviews"))
                .andExpect(status().isOk())
                .andExpect(view().name("review/reviews"))
                .andExpect(model().attributeExists("reviews"));
    }

    @Test
    void saveReviewRedirectsToListOnSuccess() throws Exception {
        when(reviewApiClient.create(any())).thenReturn(java.util.Map.of("reviewId", 1));

        mockMvc.perform(post("/view/reviews/save")
                        .param("customerId", "1")
                        .param("tripId", "2")
                        .param("rating", "5")
                        .param("comment", "Great"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/view/reviews"))
                .andExpect(flash().attributeExists("success"));
    }

    @Test
    void saveReviewRedirectsToAddFormOnBackendException() throws Exception {
        when(reviewApiClient.create(any()))
                .thenThrow(new BackendException(400, "Invalid rating"));

        mockMvc.perform(post("/view/reviews/save")
                        .param("customerId", "1")
                        .param("tripId", "2")
                        .param("rating", "5")
                        .param("comment", "Bad"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/view/reviews/add"))
                .andExpect(flash().attributeExists("error", "review"));
    }

    @Test
    void showAddReviewFormPopulatesCustomersAndTrips() throws Exception {
        when(customerApiClient.getAll()).thenReturn(List.of());
        when(tripApiClient.getAllTrips()).thenReturn(List.of());

        mockMvc.perform(get("/view/reviews/add"))
                .andExpect(status().isOk())
                .andExpect(view().name("review/add-review"))
                .andExpect(model().attributeExists("review", "customers", "trips"));
    }
}
