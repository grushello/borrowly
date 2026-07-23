package com.borrowly.dto.response;

import com.borrowly.model.rental.RentalRequestStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record RentalRequestResponse(
        UUID id,
        ItemSummaryResponse item,
        UserSummaryResponse borrower,
        LocalDate startDate,
        LocalDate endDate,
        RentalRequestStatus status,
        LocalDateTime requestedAt
) {
}