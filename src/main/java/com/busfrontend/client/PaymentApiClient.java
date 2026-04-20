package com.busfrontend.client;

import com.busfrontend.dto.PaymentRequestDTO;
import com.busfrontend.dto.PaymentResponseDTO;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Component
public class PaymentApiClient extends AbstractApiClient {

    public PaymentApiClient(RestTemplate restTemplate) { super(restTemplate); }

    public PaymentResponseDTO processPayment(PaymentRequestDTO req) {
        return post("/api/payments", req, PaymentResponseDTO.class);
    }

    public List<PaymentResponseDTO> getAll() {
        return getList("/api/payments", new ParameterizedTypeReference<List<PaymentResponseDTO>>() {});
    }

    public PaymentResponseDTO getById(Integer id) {
        return get("/api/payments/" + id, PaymentResponseDTO.class);
    }

    public PaymentResponseDTO getByBookingId(Integer bookingId) {
        return get("/api/payments/booking/" + bookingId, PaymentResponseDTO.class);
    }

    public List<PaymentResponseDTO> getByCustomerId(Integer customerId) {
        return getList("/api/payments/customer/" + customerId,
                new ParameterizedTypeReference<List<PaymentResponseDTO>>() {});
    }

    public byte[] downloadTicketByPaymentId(Integer paymentId) {
        return getBytes("/api/payments/" + paymentId + "/ticket");
    }

    public byte[] downloadGroupTicket(List<Integer> paymentIds) {
        String csv = String.join(",", paymentIds.stream().map(String::valueOf).toList());
        return getBytes("/api/payments/group-ticket?paymentIds=" + csv);
    }
}
