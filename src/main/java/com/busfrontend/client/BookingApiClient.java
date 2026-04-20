package com.busfrontend.client;

import com.busfrontend.dto.BookingRequestDTO;
import com.busfrontend.dto.BookingResponseDTO;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Component
public class BookingApiClient extends AbstractApiClient {

    public BookingApiClient(RestTemplate restTemplate) { super(restTemplate); }

    public BookingResponseDTO createBooking(BookingRequestDTO req) {
        return post("/api/bookings", req, BookingResponseDTO.class);
    }

    @SuppressWarnings("rawtypes")
    public Map getById(Integer id) {
        return get("/api/bookings/" + id, Map.class);
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getByTrip(Integer tripId) {
        return getList("/api/bookings/trip/" + tripId, new ParameterizedTypeReference<List<Map<String, Object>>>() {});
    }

    public byte[] downloadTicket(Integer bookingId) {
        return getBytes("/api/bookings/" + bookingId + "/ticket");
    }

    public byte[] downloadGroupTicket(List<Integer> bookingIds) {
        String csv = String.join(",", bookingIds.stream().map(String::valueOf).toList());
        return getBytes("/api/bookings/group-ticket?bookingIds=" + csv);
    }
}
