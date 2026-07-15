package com.borrowly.dto.response;

import com.borrowly.model.item.ItemCondition;
import com.borrowly.model.item.ItemStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record ItemSummaryResponse(
        UUID id,
        String title,
        BigDecimal pricePerDay,
        ItemCondition condition,
        ItemStatus itemStatus,
        String ownerName,
        UUID primaryImageId
) {
}