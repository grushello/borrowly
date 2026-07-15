package com.borrowly.dto.request;

import com.borrowly.model.item.ItemCondition;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateItemRequest(
        @NotBlank
        @Size(min = 3, max = 120)
        String title,

        @Size(max = 4000)
        String description,

        @NotNull
        @DecimalMin("0.00")
        BigDecimal pricePerDay,

        @NotNull
        @DecimalMin("0.00")
        BigDecimal depositAmount,

        @NotNull
        @DecimalMin("0.00")
        BigDecimal finePerDay,

        @NotNull
        ItemCondition condition,

        @NotNull
        UUID categoryId
) {
}