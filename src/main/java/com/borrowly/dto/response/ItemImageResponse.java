package com.borrowly.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record ItemImageResponse(UUID id,
                                String fileName,
                                String contentType,
                                boolean primary,
                                LocalDateTime createdAt) {
}