package com.borrowly.model.notification;

import com.borrowly.model.rental.Rental;
import com.borrowly.model.transaction.Transaction;
import com.borrowly.model.user.User;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class NotificationTest {
    private Validator validator;

    @BeforeEach
    void setUp() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Test
    void builder_ProducesExpectedValues() {
        UUID id = UUID.randomUUID();
        User recipient = createRecipient();
        Rental rental = Rental.builder().build();
        Transaction transaction = Transaction.builder().build();

        Notification notification = Notification.builder()
                .id(id)
                .message("Your rental request was approved")
                .type(NotificationType.RENTAL_APPROVED)
                .recipient(recipient)
                .rental(rental)
                .transaction(transaction)
                .build();

        assertEquals(id, notification.getId());
        assertEquals("Your rental request was approved", notification.getMessage());
        assertEquals(NotificationType.RENTAL_APPROVED, notification.getType());
        assertEquals(recipient, notification.getRecipient());
        assertEquals(rental, notification.getRental());
        assertEquals(transaction, notification.getTransaction());
    }

    @Test
    void validation_RejectsBlankMessage() {
        Notification notification = createValidNotificationBuilder()
                .message("   ")
                .build();

        Set<ConstraintViolation<Notification>> violations = validator.validate(notification);

        assertEquals(1, violations.size());
        assertEquals("message", violations.iterator().next().getPropertyPath().toString());
    }

    @Test
    void validation_RejectsNullType() {
        Notification notification = createValidNotificationBuilder()
                .type(null)
                .build();

        Set<ConstraintViolation<Notification>> violations = validator.validate(notification);

        assertEquals(1, violations.size());
        assertEquals("type", violations.iterator().next().getPropertyPath().toString());
    }

    @Test
    void validation_RejectsNullRecipient() {
        Notification notification = createValidNotificationBuilder()
                .recipient(null)
                .build();

        Set<ConstraintViolation<Notification>> violations = validator.validate(notification);

        assertEquals(1, violations.size());
        assertEquals("recipient", violations.iterator().next().getPropertyPath().toString());
    }

    @Test
    void validation_AllowsBothRentalAndTransactionNull() {
        Notification notification = createValidNotificationBuilder()
                .rental(null)
                .transaction(null)
                .build();

        Set<ConstraintViolation<Notification>> violations = validator.validate(notification);

        assertTrue(violations.isEmpty());
        assertNull(notification.getRental());
        assertNull(notification.getTransaction());
    }

    @Test
    void validation_AllowsRentalOnlySet() {
        Notification notification = createValidNotificationBuilder()
                .rental(Rental.builder().build())
                .transaction(null)
                .build();

        Set<ConstraintViolation<Notification>> violations = validator.validate(notification);

        assertTrue(violations.isEmpty());
        assertNotNull(notification.getRental());
        assertNull(notification.getTransaction());
    }

    @Test
    void validation_AllowsTransactionOnlySet() {
        Notification notification = createValidNotificationBuilder()
                .rental(null)
                .transaction(Transaction.builder().build())
                .build();

        Set<ConstraintViolation<Notification>> violations = validator.validate(notification);

        assertTrue(violations.isEmpty());
        assertNull(notification.getRental());
        assertNotNull(notification.getTransaction());
    }

    @Test
    void toString_ExcludesRecipientRentalAndTransaction() {
        Notification notification = createValidNotificationBuilder().build();

        String toStringResult = notification.toString();

        assertTrue(toStringResult.contains("message="));
        assertFalse(toStringResult.contains("recipient="), "toString MUST NOT contain the recipient field");
        assertFalse(toStringResult.contains("rental="), "toString MUST NOT contain the rental field");
        assertFalse(toStringResult.contains("transaction="), "toString MUST NOT contain the transaction field");
    }

    private Notification.NotificationBuilder createValidNotificationBuilder() {
        return Notification.builder()
                .message("General notification")
                .type(NotificationType.GENERAL)
                .recipient(createRecipient());
    }

    private User createRecipient() {
        return User.register("Jane", "Doe", "jane.doe@example.com", "hash123");
    }
}