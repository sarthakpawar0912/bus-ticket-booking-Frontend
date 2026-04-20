package com.busfrontend.controller;

import com.busfrontend.client.BookingApiClient;
import com.busfrontend.client.CustomerApiClient;
import com.busfrontend.client.PaymentApiClient;
import com.busfrontend.client.TripApiClient;
import com.busfrontend.dto.PaymentResponseDTO;
import com.busfrontend.dto.PaymentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(PaymentViewController.class)
class PaymentViewControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private PaymentApiClient paymentApiClient;
    @MockBean private BookingApiClient bookingApiClient;
    @MockBean private CustomerApiClient customerApiClient;
    @MockBean private TripApiClient tripApiClient;

    @Test
    void listPaymentsReturnsPaymentsViewWithPaymentsAndGroups() throws Exception {
        PaymentResponseDTO p = PaymentResponseDTO.builder()
                .paymentId(1).bookingId(10).customerId(2)
                .amount(new BigDecimal("500.00"))
                .paymentStatus(PaymentStatus.Success)
                .paymentDate(LocalDateTime.of(2024, 1, 1, 10, 0))
                .build();
        when(paymentApiClient.getAll()).thenReturn(List.of(p));

        mockMvc.perform(get("/view/payments"))
                .andExpect(status().isOk())
                .andExpect(view().name("payment/payments"))
                .andExpect(model().attributeExists("payments", "paymentGroups"));
    }

    @Test
    void showPaymentTicketFormReturnsTicketDownloadView() throws Exception {
        mockMvc.perform(get("/view/payments/ticket"))
                .andExpect(status().isOk())
                .andExpect(view().name("payment/ticket-download"));
    }

    @Test
    void showSuccessRendersPaymentSuccessWithComputedSiblings() throws Exception {
        LocalDateTime when = LocalDateTime.of(2024, 1, 1, 10, 0);
        PaymentResponseDTO target = PaymentResponseDTO.builder()
                .paymentId(5).bookingId(10).customerId(2)
                .amount(new BigDecimal("300.00"))
                .paymentStatus(PaymentStatus.Success)
                .paymentDate(when)
                .build();
        PaymentResponseDTO sibling = PaymentResponseDTO.builder()
                .paymentId(6).bookingId(11).customerId(2)
                .amount(new BigDecimal("300.00"))
                .paymentStatus(PaymentStatus.Success)
                .paymentDate(when)
                .build();

        when(paymentApiClient.getById(5)).thenReturn(target);
        when(paymentApiClient.getAll()).thenReturn(List.of(target, sibling));

        mockMvc.perform(get("/view/payments/success/5"))
                .andExpect(status().isOk())
                .andExpect(view().name("payment/success"))
                .andExpect(model().attributeExists("payment", "allPaymentIds",
                        "totalAmount", "perSeatFare", "seatCount"));
    }

    @Test
    void listPaymentsHandlesEmptyPaymentList() throws Exception {
        when(paymentApiClient.getAll()).thenReturn(List.of());

        mockMvc.perform(get("/view/payments"))
                .andExpect(status().isOk())
                .andExpect(view().name("payment/payments"))
                .andExpect(model().attributeExists("payments", "paymentGroups"));
    }
}
