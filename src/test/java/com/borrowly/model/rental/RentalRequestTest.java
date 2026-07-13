package com.borrowly.model.rental;

import com.borrowly.model.item.Item;
import com.borrowly.model.user.User;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.Month;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RentalRequestTest {

    private static final LocalDate START = LocalDate.of(2026, Month.JULY, 1);
    private static final LocalDate END = LocalDate.of(2026, Month.JULY, 5);

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDownValidator() {
        if (factory != null) {
            factory.close();
        }
    }

    private static RentalRequest.RentalRequestBuilder validRequest() {
        return RentalRequest.builder()
                .item(new Item())
                .borrower(new User())
                .startDate(START)
                .endDate(END)
                .status(RentalRequestStatus.PENDING);
    }

    private Set<ConstraintViolation<RentalRequest>> validate(RentalRequest request) {
        return validator.validate(request);
    }

    private void assertViolates(RentalRequest request, String property) {
        assertThat(validate(request))
                .as("expected a violation on '%s'", property)
                .anySatisfy(v -> assertThat(v.getPropertyPath().toString()).isEqualTo(property));
    }

    @Nested
    @DisplayName("builder")
    class BuilderTests {

        @Test
        @DisplayName("happy path — populates every field and validates cleanly")
        void buildsFully() {
            Item item = new Item();
            User borrower = new User();

            RentalRequest request = RentalRequest.builder()
                    .item(item)
                    .borrower(borrower)
                    .startDate(START)
                    .endDate(END)
                    .status(RentalRequestStatus.PENDING)
                    .build();

            assertThat(request.getItem()).isSameAs(item);
            assertThat(request.getBorrower()).isSameAs(borrower);
            assertThat(request.getStartDate()).isEqualTo(START);
            assertThat(request.getEndDate()).isEqualTo(END);
            assertThat(request.getStatus()).isEqualTo(RentalRequestStatus.PENDING);
            assertThat(request.isDateRangeValid()).isTrue();
            assertThat(validate(request)).isEmpty();
        }

        @Test
        @DisplayName("requestedAt is left to @CreationTimestamp, not set by the builder")
        void requestedAtIsNotSetOnBuild() {
            assertThat(validRequest().build().getRequestedAt()).isNull();
        }

        @Test
        @DisplayName("toString does not touch the lazy item/borrower relations")
        void toStringExcludesRelations() {
            String s = validRequest().build().toString();
            assertThat(s).contains("startDate=" + START);
            assertThat(s).doesNotContain("item=", "borrower=");
        }
    }

    @Nested
    @DisplayName("@PrePersist")
    class PrePersistTests {

        @Test
        @DisplayName("defaults a null status to PENDING")
        void defaultsStatusToPending() {
            RentalRequest request = validRequest().status(null).build();
            request.onCreate();
            assertThat(request.getStatus()).isEqualTo(RentalRequestStatus.PENDING);
        }

        @Test
        @DisplayName("does not overwrite an explicitly set status")
        void keepsExplicitStatus() {
            RentalRequest request = validRequest().status(RentalRequestStatus.APPROVED).build();
            request.onCreate();
            assertThat(request.getStatus()).isEqualTo(RentalRequestStatus.APPROVED);
        }
    }

    @Nested
    @DisplayName("date range")
    class DateRangeTests {

        @Test
        @DisplayName("accepts endDate strictly after startDate")
        void acceptsForwardRange() {
            RentalRequest request = validRequest().startDate(START).endDate(END).build();
            assertThat(request.isDateRangeValid()).isTrue();
            assertThat(validate(request)).isEmpty();
        }

        @Test
        @DisplayName("rejects endDate equal to startDate — a request must span at least one night")
        void rejectsSameDay() {
            RentalRequest request = validRequest().startDate(START).endDate(START).build();
            assertThat(request.isDateRangeValid()).isFalse();
            assertViolates(request, "dateRangeValid");
        }

        @Test
        @DisplayName("rejects endDate before startDate")
        void rejectsInvertedRange() {
            RentalRequest request = validRequest().startDate(END).endDate(START).build();
            assertThat(request.isDateRangeValid()).isFalse();
            assertViolates(request, "dateRangeValid");
        }

        @Test
        @DisplayName("a null startDate fails both @NotNull and @AssertTrue")
        void rejectsNullStartDate() {
            RentalRequest request = validRequest().startDate(null).build();
            assertThat(request.isDateRangeValid()).isFalse();
            assertViolates(request, "startDate");
            assertViolates(request, "dateRangeValid");
        }

        @Test
        @DisplayName("a null endDate fails both @NotNull and @AssertTrue")
        void rejectsNullEndDate() {
            RentalRequest request = validRequest().endDate(null).build();
            assertThat(request.isDateRangeValid()).isFalse();
            assertViolates(request, "endDate");
            assertViolates(request, "dateRangeValid");
        }

        @Test
        @DisplayName("both dates null still fails cleanly rather than throwing")
        void rejectsBothDatesNull() {
            RentalRequest request = validRequest().startDate(null).endDate(null).build();
            assertThat(request.isDateRangeValid()).isFalse();
            assertViolates(request, "dateRangeValid");
        }
    }

    @Nested
    @DisplayName("relations")
    class RelationTests {

        @Test
        @DisplayName("rejects a null item")
        void rejectsNullItem() {
            assertViolates(validRequest().item(null).build(), "item");
        }

        @Test
        @DisplayName("rejects a null borrower")
        void rejectsNullBorrower() {
            assertViolates(validRequest().borrower(null).build(), "borrower");
        }
    }

    @Nested
    @DisplayName("status")
    class StatusTests {

        @Test
        @DisplayName("standalone validation rejects a null status")
        void rejectsNullStatus() {
            assertViolates(validRequest().status(null).build(), "status");
        }

        @Test
        @DisplayName("but @PrePersist fills it in first, so a persisted request is valid")
        void prePersistSatisfiesNotNull() {
            RentalRequest request = validRequest().status(null).build();
            request.onCreate();
            assertThat(validate(request)).isEmpty();
        }
    }
}