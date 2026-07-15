package com.borrowly.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record ReviewResponse(
        UUID id,
        ItemSummaryResponse item,
        UserSummaryResponse reviewer,
        Integer rating,
        String comment,
        UUID rentalId,
        LocalDateTime createdAt
) {
}