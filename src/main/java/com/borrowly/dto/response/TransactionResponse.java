package com.borrowly.dto.response;

import com.borrowly.model.transaction.TransactionStatus;
import com.borrowly.model.transaction.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record TransactionResponse(
        UUID id,
        BigDecimal amount,
        TransactionType type,
        TransactionStatus status,
        String description,
        UUID rentalId,
        LocalDateTime createdAt
) {
}