package com.borrowly.model.transaction;

import com.borrowly.model.user.User;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class TransactionTest {

    private static Validator validator;

    @BeforeAll
    static void setupValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    private Transaction.TransactionBuilder validTransactionBuilder() {
        return Transaction.builder()
                .amount(new BigDecimal("100.00"))
                .type(TransactionType.TOP_UP)
                .status(TransactionStatus.COMPLETED)
                .description("Top up")
                .user(createUser());
    }

    @Test
    void shouldBuildTransaction() {
        Transaction transaction = validTransactionBuilder().build();

        assertNotNull(transaction);
        assertNotNull(transaction.getId());
        assertEquals(new BigDecimal("100.00"), transaction.getAmount());
        assertEquals(TransactionType.TOP_UP, transaction.getType());
        assertEquals(TransactionStatus.COMPLETED, transaction.getStatus());
        assertEquals("Top up", transaction.getDescription());
        assertNotNull(transaction.getUser());
        assertNull(transaction.getRental());
    }

    @Test
    void shouldGenerateIdByDefault() {
        Transaction transaction = validTransactionBuilder().build();

        assertNotNull(transaction.getId());
    }

    @Test
    void shouldRejectZeroAmount() {
        Transaction transaction = validTransactionBuilder()
                .amount(BigDecimal.ZERO)
                .build();

        Set<ConstraintViolation<Transaction>> violations = validator.validate(transaction);

        assertTrue(
                violations.stream()
                        .anyMatch(v -> v.getPropertyPath().toString().equals("amount"))
        );
    }

    @Test
    void shouldRejectNegativeAmount() {
        Transaction transaction = validTransactionBuilder()
                .amount(new BigDecimal("-1.00"))
                .build();

        Set<ConstraintViolation<Transaction>> violations = validator.validate(transaction);

        assertTrue(
                violations.stream()
                        .anyMatch(v -> v.getPropertyPath().toString().equals("amount"))
        );
    }

    @Test
    void shouldRejectNullAmount() {
        Transaction transaction = validTransactionBuilder()
                .amount(null)
                .build();

        Set<ConstraintViolation<Transaction>> violations = validator.validate(transaction);

        assertTrue(
                violations.stream()
                        .anyMatch(v -> v.getPropertyPath().toString().equals("amount"))
        );
    }

    @Test
    void shouldRejectNullType() {
        Transaction transaction = validTransactionBuilder()
                .type(null)
                .build();

        Set<ConstraintViolation<Transaction>> violations = validator.validate(transaction);

        assertTrue(
                violations.stream()
                        .anyMatch(v -> v.getPropertyPath().toString().equals("type"))
        );
    }

    @Test
    void shouldRejectNullStatus() {
        Transaction transaction = validTransactionBuilder()
                .status(null)
                .build();

        Set<ConstraintViolation<Transaction>> violations = validator.validate(transaction);

        assertTrue(
                violations.stream()
                        .anyMatch(v -> v.getPropertyPath().toString().equals("status"))
        );
    }

    @Test
    void shouldRejectNullUser() {
        Transaction transaction = validTransactionBuilder()
                .user(null)
                .build();

        Set<ConstraintViolation<Transaction>> violations = validator.validate(transaction);

        assertTrue(
                violations.stream()
                        .anyMatch(v -> v.getPropertyPath().toString().equals("user"))
        );
    }

    @Test
    void shouldAllowNullRental() {
        Transaction transaction = validTransactionBuilder()
                .rental(null)
                .build();

        Set<ConstraintViolation<Transaction>> violations = validator.validate(transaction);

        assertFalse(
                violations.stream()
                        .anyMatch(v -> v.getPropertyPath().toString().equals("rental"))
        );
    }

    @Test
    void shouldRejectDescriptionLongerThan500Characters() {
        String description = "a".repeat(501);

        Transaction transaction = validTransactionBuilder()
                .description(description)
                .build();

        Set<ConstraintViolation<Transaction>> violations = validator.validate(transaction);

        assertTrue(
                violations.stream()
                        .anyMatch(v -> v.getPropertyPath().toString().equals("description"))
        );
    }

    @Test
    void shouldAcceptDescriptionOf500Characters() {
        String description = "a".repeat(500);

        Transaction transaction = validTransactionBuilder()
                .description(description)
                .build();

        Set<ConstraintViolation<Transaction>> violations = validator.validate(transaction);

        assertTrue(violations.isEmpty());
    }

    private User createUser() {
        return User.register(
                "John",
                "Doe",
                "john.doe@example.com",
                "hashedPassword"
        );
    }
}