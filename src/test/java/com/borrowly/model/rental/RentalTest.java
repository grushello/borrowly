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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RentalTest {

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

    private static Rental.RentalBuilder validRental() {
        return Rental.builder()
                .item(new Item())
                .borrower(new User())
                .startDate(LocalDate.of(2026, Month.JULY, 1))
                .endDate(LocalDate.of(2026, Month.JULY, 5))
                .itemTitle("Bosch Drill")
                .dailyPrice(new BigDecimal("5.00"))
                .depositAmount(new BigDecimal("50.00"))
                .finePerDay(new BigDecimal("2.50"))
                .totalPrice(new BigDecimal("25.00"))
                .status(RentalStatus.ACTIVE);
    }

    private Set<ConstraintViolation<Rental>> validate(Rental rental) {
        return validator.validate(rental);
    }

    private void assertViolates(Rental rental, String property) {
        assertThat(validate(rental))
                .as("expected a violation on '%s'", property)
                .anySatisfy(v -> assertThat(v.getPropertyPath().toString()).isEqualTo(property));
    }

    @Nested
    @DisplayName("builder")
    class BuilderTests {

        @Test
        @DisplayName("populates every field it is given")
        void buildsFully() {
            Item item = new Item();
            User borrower = new User();

            Rental rental = Rental.builder()
                    .item(item)
                    .borrower(borrower)
                    .startDate(LocalDate.of(2026, Month.JULY, 1))
                    .endDate(LocalDate.of(2026, Month.JULY, 5))
                    .itemTitle("Bosch Drill")
                    .dailyPrice(new BigDecimal("5.00"))
                    .depositAmount(new BigDecimal("50.00"))
                    .finePerDay(new BigDecimal("2.50"))
                    .totalPrice(new BigDecimal("25.00"))
                    .status(RentalStatus.ACTIVE)
                    .build();

            assertThat(rental.getItem()).isSameAs(item);
            assertThat(rental.getBorrower()).isSameAs(borrower);
            assertThat(rental.getStartDate()).isEqualTo(LocalDate.of(2026, Month.JULY, 1));
            assertThat(rental.getEndDate()).isEqualTo(LocalDate.of(2026, Month.JULY, 5));
            assertThat(rental.getItemTitle()).isEqualTo("Bosch Drill");
            assertThat(rental.getDailyPrice()).isEqualByComparingTo("5.00");
            assertThat(rental.getDepositAmount()).isEqualByComparingTo("50.00");
            assertThat(rental.getFinePerDay()).isEqualByComparingTo("2.50");
            assertThat(rental.getTotalPrice()).isEqualByComparingTo("25.00");
            assertThat(rental.getStatus()).isEqualTo(RentalStatus.ACTIVE);
            assertThat(rental.getActualReturnDate()).isNull();
        }

        @Test
        @DisplayName("a fully populated Rental has no violations")
        void validRentalPasses() {
            assertThat(validate(validRental().build())).isEmpty();
        }

        @Test
        @DisplayName("toString does not touch the lazy item/borrower relations")
        void toStringExcludesRelations() {
            String s = validRental().build().toString();
            assertThat(s).contains("itemTitle=Bosch Drill");
            assertThat(s).doesNotContain("item=", "borrower=");
        }
    }

    @Nested
    @DisplayName("@PrePersist")
    class PrePersistTests {

        @Test
        @DisplayName("defaults a null status to ACTIVE")
        void defaultsStatusToActive() {
            Rental rental = validRental().status(null).build();
            rental.onCreate();
            assertThat(rental.getStatus()).isEqualTo(RentalStatus.ACTIVE);
        }

        @Test
        @DisplayName("does not overwrite an explicitly set status")
        void keepsExplicitStatus() {
            Rental rental = validRental().status(RentalStatus.OVERDUE).build();
            rental.onCreate();
            assertThat(rental.getStatus()).isEqualTo(RentalStatus.OVERDUE);
        }

        @Test
        @DisplayName("stamps createdAt when absent")
        void stampsCreatedAt() {
            Rental rental = validRental().build();
            assertThat(rental.getCreatedAt()).isNull();
            rental.onCreate();
            assertThat(rental.getCreatedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("date range")
    class DateRangeTests {

        @Test
        @DisplayName("accepts endDate after startDate")
        void acceptsNormalRange() {
            Rental rental = validRental()
                    .startDate(LocalDate.of(2026, Month.JULY, 1))
                    .endDate(LocalDate.of(2026, Month.JULY, 5))
                    .build();
            assertThat(rental.isDateRangeValid()).isTrue();
            assertThat(validate(rental)).isEmpty();
        }

        @Test
        @DisplayName("accepts a single-day rental where endDate equals startDate")
        void acceptsSameDay() {
            LocalDate day = LocalDate.of(2026, Month.JULY, 1);
            Rental rental = validRental().startDate(day).endDate(day).build();
            assertThat(rental.isDateRangeValid()).isTrue();
            assertThat(validate(rental)).isEmpty();
        }

        @Test
        @DisplayName("rejects endDate before startDate")
        void rejectsInvertedRange() {
            Rental rental = validRental()
                    .startDate(LocalDate.of(2026, Month.JULY, 5))
                    .endDate(LocalDate.of(2026, Month.JULY, 1))
                    .build();
            assertThat(rental.isDateRangeValid()).isFalse();
            assertViolates(rental, "dateRangeValid");
        }

        @Test
        @DisplayName("defers to @NotNull when a date is missing")
        void nullDatesDeferToNotNull() {
            assertThat(validRental().startDate(null).build().isDateRangeValid()).isTrue();
            assertThat(validRental().endDate(null).build().isDateRangeValid()).isTrue();
        }

        @Test
        @DisplayName("rejects a null startDate")
        void rejectsNullStartDate() {
            assertViolates(validRental().startDate(null).build(), "startDate");
        }

        @Test
        @DisplayName("rejects a null endDate")
        void rejectsNullEndDate() {
            assertViolates(validRental().endDate(null).build(), "endDate");
        }
    }

    @Nested
    @DisplayName("relations")
    class RelationTests {

        @Test
        @DisplayName("rejects a null item")
        void rejectsNullItem() {
            assertViolates(validRental().item(null).build(), "item");
        }

        @Test
        @DisplayName("rejects a null borrower")
        void rejectsNullBorrower() {
            assertViolates(validRental().borrower(null).build(), "borrower");
        }
    }

    @Nested
    @DisplayName("money fields")
    class MoneyTests {

        @ParameterizedTest(name = "{0} rejects a negative amount")
        @ValueSource(strings = {"dailyPrice", "depositAmount", "finePerDay", "totalPrice"})
        void rejectsNegativeMoney(String property) {
            Rental rental = withMoney(property, new BigDecimal("-0.01"));
            assertViolates(rental, property);
        }

        @ParameterizedTest(name = "{0} rejects null")
        @ValueSource(strings = {"dailyPrice", "depositAmount", "finePerDay", "totalPrice"})
        void rejectsNullMoney(String property) {
            Rental rental = withMoney(property, null);
            assertViolates(rental, property);
        }

        @ParameterizedTest(name = "{0} accepts zero")
        @ValueSource(strings = {"dailyPrice", "depositAmount", "finePerDay", "totalPrice"})
        void acceptsZero(String property) {
            Rental rental = withMoney(property, BigDecimal.ZERO);
            assertThat(validate(rental)).isEmpty();
        }

        private Rental withMoney(String property, BigDecimal value) {
            Rental.RentalBuilder builder = validRental();
            return switch (property) {
                case "dailyPrice" -> builder.dailyPrice(value).build();
                case "depositAmount" -> builder.depositAmount(value).build();
                case "finePerDay" -> builder.finePerDay(value).build();
                case "totalPrice" -> builder.totalPrice(value).build();
                default -> throw new IllegalArgumentException("Unknown money field: " + property);
            };
        }
    }

    @Nested
    @DisplayName("itemTitle snapshot")
    class ItemTitleTests {

        @Test
        @DisplayName("rejects a blank title")
        void rejectsBlank() {
            assertViolates(validRental().itemTitle("   ").build(), "itemTitle");
        }

        @Test
        @DisplayName("rejects a null title")
        void rejectsNull() {
            assertViolates(validRental().itemTitle(null).build(), "itemTitle");
        }

        @Test
        @DisplayName("rejects a title longer than 120 characters")
        void rejectsTooLong() {
            assertViolates(validRental().itemTitle("x".repeat(121)).build(), "itemTitle");
        }

        @Test
        @DisplayName("accepts a title of exactly 120 characters")
        void acceptsBoundary() {
            assertThat(validate(validRental().itemTitle("x".repeat(120)).build())).isEmpty();
        }
    }
}