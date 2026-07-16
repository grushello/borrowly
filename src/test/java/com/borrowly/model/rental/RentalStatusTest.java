package com.borrowly.model.rental;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RentalStatusTest {

    @Test
    @DisplayName("declares exactly ACTIVE, RETURNED, OVERDUE")
    void hasExpectedValues() {
        assertThat(RentalStatus.values())
                .containsExactly(
                        RentalStatus.ACTIVE,
                        RentalStatus.RETURNED,
                        RentalStatus.OVERDUE);
    }

    @Test
    @DisplayName("names are stable — they are persisted via @Enumerated(STRING)")
    void namesAreStable() {
        assertThat(RentalStatus.ACTIVE.name()).isEqualTo("ACTIVE");
        assertThat(RentalStatus.RETURNED.name()).isEqualTo("RETURNED");
        assertThat(RentalStatus.OVERDUE.name()).isEqualTo("OVERDUE");
    }

    @Test
    @DisplayName("valueOf round-trips every constant")
    void valueOfRoundTrips() {
        for (RentalStatus status : RentalStatus.values()) {
            assertThat(RentalStatus.valueOf(status.name())).isSameAs(status);
        }
    }
}