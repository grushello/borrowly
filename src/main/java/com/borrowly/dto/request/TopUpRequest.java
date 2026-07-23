package com.borrowly.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record TopUpRequest(

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
        @DecimalMax(value = "5000.00", message = "A single top-up cannot exceed 5000.00")
        @Digits(integer = 4, fraction = 2, message = "Amount cannot have more than 4 integer digits and 2 decimals")
        BigDecimal amount
) {
}