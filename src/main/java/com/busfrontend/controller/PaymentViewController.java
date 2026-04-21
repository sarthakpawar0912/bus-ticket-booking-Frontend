package com.busfrontend.controller;

import com.busfrontend.client.BackendException;
import com.busfrontend.client.BookingApiClient;
import com.busfrontend.client.CustomerApiClient;
import com.busfrontend.client.PaymentApiClient;
import com.busfrontend.client.TripApiClient;
import com.busfrontend.dto.PaymentGroup;
import com.busfrontend.dto.PaymentRequestDTO;
import com.busfrontend.dto.PaymentResponseDTO;
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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Controller
@RequiredArgsConstructor
public class PaymentViewController {

    private static final String ATTR_SEAT_COUNT = "seatCount";
    private static final String ATTR_ALL_PAYMENT_IDS = "allPaymentIds";
    private static final String ATTR_ERROR = "error";

    private final PaymentApiClient paymentApiClient;
    private final BookingApiClient bookingApiClient;
    private final CustomerApiClient customerApiClient;
    private final TripApiClient tripApiClient;

    /**
     * Proxy the backend PDF ticket through the frontend so that the URL
     * {@code /api/payments/{id}/ticket} works on port 8081 (legacy links that
     * predate the split still land here).
     */
    @GetMapping({"/api/payments/{id}/ticket", "/view/payments/{id}/download"})
    public ResponseEntity<byte[]> downloadPaymentTicket(@PathVariable Integer id) {
        byte[] pdf = paymentApiClient.downloadTicketByPaymentId(id);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "ticket-" + id + ".pdf");
        return ResponseEntity.ok().headers(headers).body(pdf);
    }

    /** Proxy group ticket download by paymentIds CSV. */
    @GetMapping({"/api/payments/group-ticket", "/view/payments/group-ticket-download"})
    public ResponseEntity<byte[]> downloadGroupTicket(@RequestParam("paymentIds") String paymentIdsCsv) {
        List<Integer> ids = new ArrayList<>();
        for (String s : paymentIdsCsv.split(",")) {
            s = s.trim();
            if (s.isEmpty()) continue;
            try {
                ids.add(Integer.parseInt(s));
            } catch (NumberFormatException ex) {
                return ResponseEntity.badRequest().build();
            }
        }
        if (ids.isEmpty()) return ResponseEntity.badRequest().build();
        byte[] pdf = paymentApiClient.downloadGroupTicket(ids);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "group-ticket.pdf");
        return ResponseEntity.ok().headers(headers).body(pdf);
    }

    @GetMapping("/view/payments")
    public String listPayments(Model model) {
        List<PaymentResponseDTO> all = paymentApiClient.getAll();
        model.addAttribute("payments", all);
        model.addAttribute("paymentGroups", groupRelatedPayments(all));
        return "payment/payments";
    }

    /**
     * Groups payments that were processed together (same customer + same
     * second-level timestamp + same status). Collapses a multi-seat payment
     * run into a single row.
     */
    private List<PaymentGroup> groupRelatedPayments(List<PaymentResponseDTO> all) {
        if (all == null) return List.of();
        Map<String, List<PaymentResponseDTO>> buckets = new LinkedHashMap<>();
        for (PaymentResponseDTO p : all) {
            String ts = p.getPaymentDate() == null ? "null"
                    : p.getPaymentDate().withNano(0).toString();
            String key = p.getCustomerId() + "|" + ts + "|" + p.getPaymentStatus();
            buckets.computeIfAbsent(key, k -> new ArrayList<>()).add(p);
        }
        List<PaymentGroup> result = new ArrayList<>();
        for (List<PaymentResponseDTO> bucket : buckets.values()) {
            PaymentResponseDTO first = bucket.get(0);
            BigDecimal total = bucket.stream()
                    .map(PaymentResponseDTO::getAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            result.add(PaymentGroup.builder()
                    .paymentIds(bucket.stream().map(PaymentResponseDTO::getPaymentId).toList())
                    .bookingIds(bucket.stream().map(PaymentResponseDTO::getBookingId).toList())
                    .customerId(first.getCustomerId())
                    .totalAmount(total)
                    .paymentStatus(first.getPaymentStatus())
                    .paymentDate(first.getPaymentDate())
                    .seatCount(bucket.size())
                    .build());
        }
        result.sort((a, b) -> {
            if (a.getPaymentDate() == null && b.getPaymentDate() == null) return 0;
            if (a.getPaymentDate() == null) return 1;
            if (b.getPaymentDate() == null) return -1;
            return b.getPaymentDate().compareTo(a.getPaymentDate());
        });
        return result;
    }

    @GetMapping("/view/payments/pay/{bookingId}")
    public String showCheckout(@PathVariable Integer bookingId, Model model) {
        @SuppressWarnings("rawtypes")
        Map booking = bookingApiClient.getById(bookingId);
        BigDecimal fare = resolveFareFromBooking(booking);
        Object seatNumber = booking == null ? null : booking.get("seatNumber");
        model.addAttribute("bookingIds", String.valueOf(bookingId));
        model.addAttribute(ATTR_SEAT_COUNT, 1);
        model.addAttribute("seatNumbers", String.valueOf(seatNumber));
        model.addAttribute("amount", fare);
        model.addAttribute("customers", customerApiClient.getAll());
        return "payment/checkout";
    }

    @GetMapping("/view/payments/pay-all")
    public String showCheckoutAll(@RequestParam String bookingIds,
                                  @RequestParam(required = false) Integer customerId,
                                  Model model) {
        String[] ids = bookingIds.split(",");
        BigDecimal totalFare = BigDecimal.ZERO;
        StringBuilder seatNums = new StringBuilder();
        for (String idStr : ids) {
            @SuppressWarnings("rawtypes")
            Map booking = bookingApiClient.getById(Integer.parseInt(idStr.trim()));
            BigDecimal fare = resolveFareFromBooking(booking);
            if (fare != null) totalFare = totalFare.add(fare);
            Object seat = booking == null ? null : booking.get("seatNumber");
            if (!seatNums.isEmpty()) seatNums.append(", ");
            seatNums.append(seat);
        }
        model.addAttribute("bookingIds", bookingIds);
        model.addAttribute(ATTR_SEAT_COUNT, ids.length);
        model.addAttribute("seatNumbers", seatNums.toString());
        model.addAttribute("amount", totalFare);
        model.addAttribute("preSelectedCustomerId", customerId);
        model.addAttribute("customers", customerApiClient.getAll());
        return "payment/checkout";
    }

    /**
     * Fans out N bookings into N PaymentRequestDTO calls. Each booking is
     * charged its per-seat fare so the sum equals the user-supplied total.
     */
    @PostMapping("/view/payments/process")
    public String processPaymentView(@RequestParam String bookingIds,
                                     @RequestParam Integer customerId,
                                     @RequestParam BigDecimal amount,
                                     RedirectAttributes ra) {
        try {
            String[] idStrs = bookingIds.split(",");
            List<Integer> bookingIdList = new ArrayList<>();
            for (String s : idStrs) bookingIdList.add(Integer.parseInt(s.trim()));

            int n = bookingIdList.size();
            if (n == 0) throw new IllegalArgumentException("No bookings supplied");

            BigDecimal perSeatFare = amount.divide(BigDecimal.valueOf(n), 2, java.math.RoundingMode.HALF_UP);

            List<PaymentResponseDTO> responses = new ArrayList<>();
            for (Integer bid : bookingIdList) {
                PaymentRequestDTO req = new PaymentRequestDTO(bid, customerId, perSeatFare);
                responses.add(paymentApiClient.processPayment(req));
            }

            List<Integer> paymentIds = responses.stream()
                    .map(PaymentResponseDTO::getPaymentId)
                    .toList();
            if (paymentIds.isEmpty()) {
                throw new IllegalStateException("No payments were processed");
            }
            Integer firstPaymentId = paymentIds.get(0);

            ra.addFlashAttribute(ATTR_ALL_PAYMENT_IDS, paymentIds);
            ra.addFlashAttribute("totalAmount", amount);
            ra.addFlashAttribute("perSeatFare", perSeatFare);
            ra.addFlashAttribute(ATTR_SEAT_COUNT, n);
            ra.addFlashAttribute("allBookingIds", bookingIds);
            return "redirect:/view/payments/success/" + firstPaymentId;
        } catch (BackendException ex) {
            ra.addFlashAttribute(ATTR_ERROR, ex.getMessage());
            return "redirect:/view/payments/pay-all?bookingIds=" + bookingIds;
        } catch (Exception ex) {
            ra.addFlashAttribute(ATTR_ERROR, ex.getMessage());
            return "redirect:/view/payments/pay-all?bookingIds=" + bookingIds;
        }
    }

    @GetMapping("/view/payments/ticket")
    public String showPaymentTicketForm() {
        return "payment/ticket-download";
    }

    @GetMapping("/view/payments/success/{paymentId}")
    public String showSuccess(@PathVariable Integer paymentId, Model model) {
        PaymentResponseDTO payment = paymentApiClient.getById(paymentId);
        model.addAttribute("payment", payment);

        if (!model.containsAttribute(ATTR_ALL_PAYMENT_IDS)) {
            List<PaymentResponseDTO> siblings = paymentApiClient.getAll().stream()
                    .filter(p -> Objects.equals(p.getCustomerId(), payment.getCustomerId())
                            && p.getPaymentDate() != null && payment.getPaymentDate() != null
                            && p.getPaymentDate().withNano(0).equals(payment.getPaymentDate().withNano(0))
                            && p.getPaymentStatus() == payment.getPaymentStatus())
                    .toList();
            List<Integer> paymentIds = siblings.stream().map(PaymentResponseDTO::getPaymentId).toList();
            BigDecimal total = siblings.stream()
                    .map(PaymentResponseDTO::getAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            model.addAttribute(ATTR_ALL_PAYMENT_IDS, paymentIds.isEmpty() ? List.of(paymentId) : paymentIds);
            model.addAttribute("totalAmount", total.signum() > 0 ? total : payment.getAmount());
            model.addAttribute("perSeatFare", payment.getAmount());
            model.addAttribute(ATTR_SEAT_COUNT, Math.max(1, siblings.size()));
        }
        return "payment/success";
    }

    /**
     * Booking JSON looks like {"bookingId":N,"tripId":N,"seatNumber":N,"status":"..."}.
     * The backend version read fare directly off the Booking entity; here we
     * look up the trip to get fare.
     */
    @SuppressWarnings("rawtypes")
    private BigDecimal resolveFareFromBooking(Map booking) {
        if (booking == null) return null;
        Object tripIdObj = booking.get("tripId");
        if (!(tripIdObj instanceof Number n)) return null;
        try {
            TripDTO trip = tripApiClient.getById(n.intValue());
            return trip != null ? trip.getFare() : null;
        } catch (Exception ex) {
            return null;
        }
    }
}
