package com.busfrontend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * View-model used by the Payments list page. Groups related Payment rows
 * (same customer + same second-precision timestamp) so that a multi-seat
 * booking shows as ONE row with a single Download Group Ticket action
 * instead of N separate ticket buttons.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentGroup {
    private List<Integer> paymentIds;
    private List<Integer> bookingIds;
    private Integer customerId;
    private BigDecimal totalAmount;
    private PaymentStatus paymentStatus;
    private LocalDateTime paymentDate;
    private int seatCount;

    /** First paymentId - used when only ONE payment exists in the group. */
    public Integer getFirstPaymentId() {
        return paymentIds == null || paymentIds.isEmpty() ? null : paymentIds.get(0);
    }

    /** True when this is a multi-seat group booking. */
    public boolean isGroup() {
        return paymentIds != null && paymentIds.size() > 1;
    }
}
