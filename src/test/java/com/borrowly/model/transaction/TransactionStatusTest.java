package com.borrowly.model.transaction;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TransactionStatusTest {

    @Test
    void shouldContainExpectedValues() {
        assertArrayEquals(
                new TransactionStatus[]{
                        TransactionStatus.COMPLETED,
                        TransactionStatus.FAILED
                },
                TransactionStatus.values()
        );
    }

    @Test
    void shouldResolveValueOf() {
        assertEquals(
                TransactionStatus.COMPLETED,
                TransactionStatus.valueOf("COMPLETED")
        );

        assertEquals(
                TransactionStatus.FAILED,
                TransactionStatus.valueOf("FAILED")
        );
    }

    @Test
    void shouldThrowForInvalidValue() {
        assertThrows(
                IllegalArgumentException.class,
                () -> TransactionStatus.valueOf("UNKNOWN")
        );
    }
}