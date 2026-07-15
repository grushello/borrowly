package com.borrowly.mapper;

import com.borrowly.dto.response.NotificationResponse;
import com.borrowly.model.notification.Notification;
import com.borrowly.model.notification.NotificationType;
import com.borrowly.model.rental.Rental;
import com.borrowly.model.transaction.Transaction;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class NotificationMapperTest {

    private final NotificationMapper mapper = Mappers.getMapper(NotificationMapper.class);

    @Test
    void toResponse_mapsEntityWithRentalAndTransaction() {
        UUID rentalId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();

        Rental rental = Rental.builder().id(rentalId).build();
        Transaction transaction = Transaction.builder().id(transactionId).build();

        Notification notification = Notification.builder()
                .message("Your rental was approved")
                .type(NotificationType.RENTAL_APPROVED)
                .rental(rental)
                .transaction(transaction)
                .build();

        NotificationResponse response = mapper.toResponse(notification);

        assertThat(response.id()).isEqualTo(notification.getId());
        assertThat(response.message()).isEqualTo("Your rental was approved");
        assertThat(response.type()).isEqualTo(NotificationType.RENTAL_APPROVED);
        assertThat(response.rentalId()).isEqualTo(rentalId);
        assertThat(response.transactionId()).isEqualTo(transactionId);
        assertThat(response.createdAt()).isEqualTo(notification.getCreatedAt());
    }

    @Test
    void toResponse_nullRentalAndTransactionProduceNullIds() {
        Notification notification = Notification.builder()
                .message("General announcement")
                .type(NotificationType.GENERAL)
                .build();

        NotificationResponse response = mapper.toResponse(notification);

        assertThat(response.rentalId()).isNull();
        assertThat(response.transactionId()).isNull();
        assertThat(response.id()).isNotNull();
        assertThat(response.message()).isEqualTo("General announcement");
        assertThat(response.type()).isEqualTo(NotificationType.GENERAL);
    }
}