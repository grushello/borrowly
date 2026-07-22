package com.borrowly.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record UserProfileResponse(
        UUID id,
        String firstName,
        String lastName,
        LocalDateTime createdAt,
        List<ItemSummaryResponse> items,
        List<ReviewResponse> reviews,
        Double averageRating,
        long reviewCount
) {
}