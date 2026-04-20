package com.busfrontend.controller;

import com.busfrontend.client.BackendException;
import com.busfrontend.client.BookingApiClient;
import com.busfrontend.client.BusApiClient;
import com.busfrontend.client.CustomerApiClient;
import com.busfrontend.client.TripApiClient;
import com.busfrontend.dto.BookingRequestDTO;
import com.busfrontend.dto.BookingResponseDTO;
import com.busfrontend.dto.BusResponseDTO;
import com.busfrontend.dto.CustomerResponseDTO;
import com.busfrontend.dto.TripDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class BookingViewController {

    private static final String ATTR_BOOKING = "booking";
    private static final String ATTR_ERROR = "error";

    private final BookingApiClient bookingApiClient;
    private final TripApiClient tripApiClient;
    private final CustomerApiClient customerApiClient;
    private final BusApiClient busApiClient;

    @GetMapping("/view/bookings")
    public String listAvailableTrips(Model model) {
        List<TripDTO> trips = tripApiClient.getAllTrips().stream()
                .filter(trip -> trip.getAvailableSeats() != null && trip.getAvailableSeats() > 0)
                .toList();
        model.addAttribute("trips", trips);
        return "booking/trips";
    }

    @GetMapping("/view/bookings/trip/{tripId}")
    public String showSeatSelection(@PathVariable Integer tripId, Model model, RedirectAttributes ra) {
        try {
            TripDTO trip = tripApiClient.getById(tripId);

            // Resolve bus capacity via BusApiClient (TripDTO doesn't carry capacity).
            int capacity = 0;
            if (trip.getBusId() != null) {
                BusResponseDTO bus = busApiClient.getById(trip.getBusId());
                if (bus != null && bus.getCapacity() != null) capacity = bus.getCapacity();
            }

            List<Integer> allSeats = new ArrayList<>();
            for (int i = 1; i <= capacity; i++) allSeats.add(i);

            List<Integer> bookedSeats = new ArrayList<>();
            List<Map<String, Object>> bookings = bookingApiClient.getByTrip(tripId);
            if (bookings != null) {
                for (Map<String, Object> b : bookings) {
                    Object status = b.get("status");
                    Object seatNum = b.get("seatNumber");
                    if ("Booked".equals(String.valueOf(status)) && seatNum instanceof Number n) {
                        bookedSeats.add(n.intValue());
                    }
                }
            }

            model.addAttribute("trip", trip);
            model.addAttribute("allSeats", allSeats);
            model.addAttribute("bookedSeats", bookedSeats);
            model.addAttribute("customers", customerApiClient.getAll());
            return "booking/select-seat";
        } catch (BackendException ex) {
            ra.addFlashAttribute(ATTR_ERROR, ex.getMessage());
            return "redirect:/view/bookings";
        } catch (Exception ex) {
            ra.addFlashAttribute(ATTR_ERROR, ex.getMessage());
            return "redirect:/view/bookings";
        }
    }

    @PostMapping("/view/bookings/book")
    public String bookSeats(@RequestParam Integer tripId,
                            @RequestParam List<Integer> seatNumbers,
                            @RequestParam Integer customerId,
                            RedirectAttributes ra) {
        try {
            BookingRequestDTO request = BookingRequestDTO.builder()
                    .tripId(tripId).seatNumbers(seatNumbers).customerId(customerId).build();
            BookingResponseDTO response = bookingApiClient.createBooking(request);
            ra.addFlashAttribute(ATTR_BOOKING, response);
            ra.addFlashAttribute("seatNumbers", seatNumbers);

            // Pass customer name
            CustomerResponseDTO customer = customerApiClient.getById(customerId);
            if (customer != null) ra.addFlashAttribute("customerName", customer.getName());

            // Pass trip info
            TripDTO trip = tripApiClient.getById(tripId);
            if (trip != null) {
                if (trip.getFromCity() != null) ra.addFlashAttribute("fromCity", trip.getFromCity());
                if (trip.getToCity() != null) ra.addFlashAttribute("toCity", trip.getToCity());
                if (trip.getTripDate() != null) {
                    ra.addFlashAttribute("tripDate", trip.getTripDate().toLocalDate().toString());
                }
            }
            return "redirect:/view/bookings/confirmation/" + response.getBookingIds().get(0);
        } catch (BackendException ex) {
            ra.addFlashAttribute(ATTR_ERROR, ex.getMessage());
            return "redirect:/view/bookings/trip/" + tripId;
        } catch (Exception ex) {
            ra.addFlashAttribute(ATTR_ERROR, ex.getMessage());
            return "redirect:/view/bookings/trip/" + tripId;
        }
    }

    // ---- Single Ticket Download page ----
    @GetMapping("/view/bookings/ticket")
    public String showTicketDownloadForm() {
        return "booking/ticket-download";
    }

    // ---- Group Ticket Download page ----
    @GetMapping("/view/bookings/group-ticket")
    public String showGroupTicketDownloadForm() {
        return "booking/group-ticket-download";
    }

    /** Proxy backend booking PDF so legacy /api/bookings/{id}/ticket URLs work on :8081. */
    @GetMapping("/api/bookings/{id}/ticket")
    public ResponseEntity<byte[]> downloadBookingTicket(@PathVariable Integer id) {
        byte[] pdf = bookingApiClient.downloadTicket(id);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "booking-ticket-" + id + ".pdf");
        return ResponseEntity.ok().headers(headers).body(pdf);
    }

    @GetMapping("/api/bookings/group-ticket")
    public ResponseEntity<byte[]> downloadBookingGroupTicket(@RequestParam("bookingIds") String bookingIdsCsv) {
        List<Integer> ids = new ArrayList<>();
        for (String s : bookingIdsCsv.split(",")) {
            s = s.trim();
            if (!s.isEmpty()) ids.add(Integer.parseInt(s));
        }
        byte[] pdf = bookingApiClient.downloadGroupTicket(ids);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "booking-group-ticket.pdf");
        return ResponseEntity.ok().headers(headers).body(pdf);
    }

    @GetMapping("/view/bookings/confirmation/{bookingId}")
    public String showConfirmation(@PathVariable Integer bookingId, Model model) {
        // TODO: advanced flow — integrate backend ConfirmationContext later.
        // When flash attrs are missing (direct URL access), fall back to a minimal
        // view built from a single booking lookup.
        if (!model.containsAttribute(ATTR_BOOKING)) {
            try {
                @SuppressWarnings("rawtypes")
                Map booking = bookingApiClient.getById(bookingId);
                BookingResponseDTO dto = BookingResponseDTO.builder()
                        .bookingIds(List.of(bookingId))
                        .message("Booking details")
                        .build();
                model.addAttribute(ATTR_BOOKING, dto);
                if (booking != null && booking.get("seatNumber") instanceof Number seat) {
                    model.addAttribute("seatNumbers", List.of(seat.intValue()));
                }
            } catch (Exception ignored) {
                // Swallow — confirmation template tolerates missing attrs.
            }
        }
        return "booking/confirmation";
    }
}
