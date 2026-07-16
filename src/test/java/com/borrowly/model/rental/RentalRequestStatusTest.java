package com.borrowly.model.rental;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RentalRequestStatusTest {

    @Test
    @DisplayName("declares exactly PENDING, APPROVED, REJECTED, CANCELED")
    void hasExpectedValues() {
        assertThat(RentalRequestStatus.values())
                .containsExactly(
                        RentalRequestStatus.PENDING,
                        RentalRequestStatus.APPROVED,
                        RentalRequestStatus.REJECTED,
                        RentalRequestStatus.CANCELED);
    }

    @Test
    @DisplayName("names are stable — they are persisted via @Enumerated(STRING)")
    void namesAreStable() {
        assertThat(RentalRequestStatus.PENDING.name()).isEqualTo("PENDING");
        assertThat(RentalRequestStatus.APPROVED.name()).isEqualTo("APPROVED");
        assertThat(RentalRequestStatus.REJECTED.name()).isEqualTo("REJECTED");
        assertThat(RentalRequestStatus.CANCELED.name()).isEqualTo("CANCELED");
    }

    @Test
    @DisplayName("valueOf round-trips every constant")
    void valueOfRoundTrips() {
        for (RentalRequestStatus status : RentalRequestStatus.values()) {
            assertThat(RentalRequestStatus.valueOf(status.name())).isSameAs(status);
        }
    }
}