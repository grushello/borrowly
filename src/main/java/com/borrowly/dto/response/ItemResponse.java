package com.borrowly.dto.response;

import com.borrowly.model.item.ItemCondition;
import com.borrowly.model.item.ItemStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ItemResponse(
        UUID id,
        String title,
        String description,
        BigDecimal pricePerDay,
        BigDecimal depositAmount,
        BigDecimal finePerDay,
        ItemCondition condition,
        ItemStatus itemStatus,
        UserSummaryResponse owner,
        CategoryResponse category,
        List<ItemImageResponse> images,
        Double averageRating,
        long reviewCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}