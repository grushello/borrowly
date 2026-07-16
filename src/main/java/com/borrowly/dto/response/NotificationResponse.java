package com.borrowly.dto.response;

import com.borrowly.model.notification.NotificationType;

import java.time.LocalDateTime;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        String message,
        NotificationType type,
        UUID rentalId,
        UUID transactionId,
        LocalDateTime createdAt
) {}