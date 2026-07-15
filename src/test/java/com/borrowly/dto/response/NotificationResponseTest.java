package com.borrowly.dto.response;

import com.borrowly.model.notification.NotificationType;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class NotificationResponseTest {

    private static final LocalDateTime FIXED_TIME = LocalDateTime.of(2025, Month.JULY, 1, 10, 0);

    @Test
    void createsResponseWithAllFields() {
        UUID id = UUID.randomUUID();
        UUID rentalId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();
        LocalDateTime createdAt = FIXED_TIME;

        NotificationResponse response = new NotificationResponse(
                id, "Rental approved", NotificationType.RENTAL_APPROVED,
                rentalId, transactionId, createdAt
        );

        assertThat(response.id()).isEqualTo(id);
        assertThat(response.message()).isEqualTo("Rental approved");
        assertThat(response.type()).isEqualTo(NotificationType.RENTAL_APPROVED);
        assertThat(response.rentalId()).isEqualTo(rentalId);
        assertThat(response.transactionId()).isEqualTo(transactionId);
        assertThat(response.createdAt()).isEqualTo(createdAt);
    }

    @Test
    void allowsNullRentalIdAndTransactionId() {
        NotificationResponse response = new NotificationResponse(
                UUID.randomUUID(), "General notice", NotificationType.GENERAL,
                null, null, FIXED_TIME
        );

        assertThat(response.rentalId()).isNull();
        assertThat(response.transactionId()).isNull();
    }

    @Test
    void equalityBasedOnAllFields() {
        UUID id = UUID.randomUUID();
        LocalDateTime createdAt = FIXED_TIME;

        NotificationResponse a = new NotificationResponse(
                id, "msg", NotificationType.FINE, null, null, createdAt
        );
        NotificationResponse b = new NotificationResponse(
                id, "msg", NotificationType.FINE, null, null, createdAt
        );

        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
    }
}