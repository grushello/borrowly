package com.borrowly.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record CreateRentalRequest(
        @NotNull
        UUID itemId,

        @NotNull
        @FutureOrPresent
        LocalDate startDate,

        @NotNull
        @Future
        LocalDate endDate
) {
        @AssertTrue(message = "End date must be after start date")
        public boolean isEndAfterStart() {
                if (startDate == null || endDate == null) {
                        return true;
                }
                return endDate.isAfter(startDate);
        }

}