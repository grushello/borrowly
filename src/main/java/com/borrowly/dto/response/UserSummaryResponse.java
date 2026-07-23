package com.borrowly.dto.response;

import java.util.UUID;

public record UserSummaryResponse(
        UUID id,
        String firstName,
        String lastName
) {
}
