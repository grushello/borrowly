package com.borrowly.dto.response;

import com.borrowly.model.rental.RentalStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record RentalResponse(
        UUID id,
        ItemSummaryResponse item,
        UserSummaryResponse borrower,
        LocalDate startDate,
        LocalDate endDate,
        LocalDate actualReturnDate,
        String itemTitle,
        BigDecimal dailyPrice,
        BigDecimal depositAmount,
        BigDecimal finePerDay,
        BigDecimal totalPrice,
        RentalStatus status,
        LocalDateTime createdAt
) {
}