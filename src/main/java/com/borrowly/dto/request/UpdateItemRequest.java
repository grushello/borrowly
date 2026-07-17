package com.borrowly.dto.request;

import com.borrowly.model.item.ItemCondition;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record UpdateItemRequest(
        @Size(min = 3, max = 120)
        String title,

        @Size(max = 4000)
        String description,

        @DecimalMin("0.00")
        BigDecimal pricePerDay,

        @DecimalMin("0.00")
        BigDecimal depositAmount,

        @DecimalMin("0.00")
        BigDecimal finePerDay,

        ItemCondition condition,

        UUID categoryId
) {
}