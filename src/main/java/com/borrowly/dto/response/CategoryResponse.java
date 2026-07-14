package com.borrowly.dto.response;

import java.util.UUID;

public record CategoryResponse(
        UUID id,
        String name,
        String description
){
}
