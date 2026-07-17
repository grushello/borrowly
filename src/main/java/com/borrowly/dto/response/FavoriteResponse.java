package com.borrowly.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record FavoriteResponse(
        UUID id,
        ItemSummaryResponse item,
        LocalDateTime createdAt
) {
}