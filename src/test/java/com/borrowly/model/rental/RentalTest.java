package com.borrowly.model.rental;

import com.borrowly.model.item.Item;
import com.borrowly.model.item.ItemCondition;
import com.borrowly.model.user.User;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

class RentalTest {

    private static final LocalDate START = LocalDate.of(2026, Month.JULY, 1);
    private static final LocalDate END = LocalDate.of(2026, Month.JULY, 5);

    private static ValidatorFactory factory;
    private static Validator validator;

    private User borrower;
    private Item item;

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

    @BeforeEach
    void setUp() {
        borrower = User.register("Bo", "Borrower", "borrower@borrowly.test", "hash");

        item = Item.builder()
                .title("Bosch Drill")
                .description("Corded hammer drill")
                .pricePerDay(new BigDecimal("5.00"))
                .depositAmount(new BigDecimal("50.00"))
                .finePerDay(new BigDecimal("2.50"))
                .condition(ItemCondition.GOOD)
                .build();
    }

    private Rental.RentalBuilder validRental() {
        return Rental.builder()
                .item(item)
                .borrower(borrower)
                .startDate(START)
                .endDate(END)
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
            Rental rental = validRental().build();

            assertThat(rental.getId()).isNotNull();
            assertThat(rental.getItem()).isSameAs(item);
            assertThat(rental.getBorrower()).isSameAs(borrower);
            assertThat(rental.getStartDate()).isEqualTo(START);
            assertThat(rental.getEndDate()).isEqualTo(END);
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
        @DisplayName("defaults status to ACTIVE when not set")
        void defaultsStatusToActive() {
            Rental rental = Rental.builder()
                    .item(item)
                    .borrower(borrower)
                    .startDate(START)
                    .endDate(END)
                    .itemTitle("Bosch Drill")
                    .dailyPrice(new BigDecimal("5.00"))
                    .depositAmount(new BigDecimal("50.00"))
                    .finePerDay(new BigDecimal("2.50"))
                    .totalPrice(new BigDecimal("25.00"))
                    .build();

            assertThat(rental.getStatus()).isEqualTo(RentalStatus.ACTIVE);
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
    @DisplayName("returnItem")
    class ReturnItemTests {

        @Test
        @DisplayName("sets the return date and the RETURNED status together")
        void setsDateAndStatus() {
            Rental rental = validRental().build();
            LocalDate returnedOn = LocalDate.of(2026, Month.JULY, 4);

            rental.returnItem(returnedOn);

            assertThat(rental.getActualReturnDate()).isEqualTo(returnedOn);
            assertThat(rental.getStatus()).isEqualTo(RentalStatus.RETURNED);
            assertThat(validate(rental)).isEmpty();
        }

        @Test
        @DisplayName("no setStatus — a rental cannot be marked RETURNED without a return date")
        void statusHasNoSetter() {
            assertThat(Rental.class.getMethods())
                    .extracting(Method::getName)
                    .doesNotContain("setStatus", "setActualReturnDate");
        }

        @Test
        @DisplayName("the snapshot fields have no setters either")
        void snapshotFieldsHaveNoSetters() {
            assertThat(Rental.class.getMethods())
                    .extracting(Method::getName)
                    .doesNotContain(
                            "setItemTitle", "setDailyPrice", "setDepositAmount",
                            "setFinePerDay", "setTotalPrice");
        }
    }

    @Nested
    @DisplayName("markOverdue")
    class MarkOverdueTests {

        @Test
        @DisplayName("flips an ACTIVE rental to OVERDUE without touching the return date")
        void marksActiveRentalOverdue() {
            Rental rental = validRental().build();

            rental.markOverdue();

            assertThat(rental.getStatus()).isEqualTo(RentalStatus.OVERDUE);
            assertThat(rental.getActualReturnDate())
                    .as("going overdue does not mean the item came back")
                    .isNull();
            assertThat(validate(rental)).isEmpty();
        }

        @Test
        @DisplayName("refuses to mark a RETURNED rental overdue — that would un-return it")
        void rejectsAlreadyReturned() {
            Rental rental = validRental().build();
            rental.returnItem(LocalDate.of(2026, Month.JULY, 4));

            assertThatIllegalStateException()
                    .isThrownBy(rental::markOverdue)
                    .withMessageContaining("RETURNED");

            assertThat(rental.getStatus())
                    .as("the rejected call must not have changed anything")
                    .isEqualTo(RentalStatus.RETURNED);
            assertThat(rental.getActualReturnDate()).isEqualTo(LocalDate.of(2026, Month.JULY, 4));
        }

        @Test
        @DisplayName("refuses to mark an already-OVERDUE rental overdue again")
        void rejectsAlreadyOverdue() {
            Rental rental = validRental().build();
            rental.markOverdue();

            assertThatIllegalStateException()
                    .isThrownBy(rental::markOverdue)
                    .withMessageContaining("OVERDUE");

            assertThat(rental.getStatus()).isEqualTo(RentalStatus.OVERDUE);
        }

        @Test
        @DisplayName("OVERDUE is reachable ONLY through markOverdue — there is no setter")
        void overdueHasNoOtherRoute() {
            assertThat(Rental.class.getMethods())
                    .extracting(Method::getName)
                    .doesNotContain("setStatus");
        }
    }

    @Nested
    @DisplayName("returning an overdue rental")
    class LateReturnTests {

        @Test
        @DisplayName("an OVERDUE rental can still be returned — this is the normal late-return path")
        void overdueRentalCanBeReturned() {
            Rental rental = validRental().build();
            rental.markOverdue();
            assertThat(rental.getStatus()).isEqualTo(RentalStatus.OVERDUE);

            LocalDate returnedLate = END.plusDays(3);
            rental.returnItem(returnedLate);

            assertThat(rental.getStatus())
                    .as("OVERDUE must not be a dead end")
                    .isEqualTo(RentalStatus.RETURNED);
            assertThat(rental.getActualReturnDate()).isEqualTo(returnedLate);
            assertThat(validate(rental)).isEmpty();
        }

        @Test
        @DisplayName("the full lifecycle ACTIVE -> OVERDUE -> RETURNED holds together")
        void fullLateLifecycle() {
            Rental rental = validRental().build();

            assertThat(rental.getStatus()).isEqualTo(RentalStatus.ACTIVE);
            assertThat(rental.getActualReturnDate()).isNull();

            rental.markOverdue();
            assertThat(rental.getStatus()).isEqualTo(RentalStatus.OVERDUE);
            assertThat(rental.getActualReturnDate()).isNull();

            rental.returnItem(END.plusDays(2));
            assertThat(rental.getStatus()).isEqualTo(RentalStatus.RETURNED);
            assertThat(rental.getActualReturnDate()).isEqualTo(END.plusDays(2));
        }
    }

    @Nested
    @DisplayName("@PrePersist")
    class PrePersistTests {

        @Test
        @DisplayName("sets status to ACTIVE if null")
        void defaultsNullStatusToActive() {
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
    }

    @Nested
    @DisplayName("date range")
    class DateRangeTests {

        @Test
        @DisplayName("accepts endDate after startDate")
        void acceptsNormalRange() {
            Rental rental = validRental().build();
            assertThat(rental.isDateRangeValid()).isTrue();
            assertThat(validate(rental)).isEmpty();
        }

        @Test
        @DisplayName("accepts a single-day rental")
        void acceptsSameDay() {
            Rental rental = validRental().startDate(START).endDate(START).build();
            assertThat(rental.isDateRangeValid()).isTrue();
            assertThat(validate(rental)).isEmpty();
        }

        @Test
        @DisplayName("rejects endDate before startDate")
        void rejectsInvertedRange() {
            Rental rental = validRental().startDate(END).endDate(START).build();
            assertThat(rental.isDateRangeValid()).isFalse();
            assertViolates(rental, "dateRangeValid");
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
    @DisplayName("actual return date")
    class ActualReturnDateTests {

        @Test
        @DisplayName("accepts a null actualReturnDate — the item is still out")
        void acceptsNotYetReturned() {
            Rental rental = validRental().build();
            assertThat(rental.getActualReturnDate()).isNull();
            assertThat(rental.isActualReturnDateValid()).isTrue();
            assertThat(validate(rental)).isEmpty();
        }

        @Test
        @DisplayName("accepts a return on the start date")
        void acceptsReturnOnStartDate() {
            Rental rental = validRental().actualReturnDate(START).build();
            assertThat(rental.isActualReturnDateValid()).isTrue();
            assertThat(validate(rental)).isEmpty();
        }

        @Test
        @DisplayName("accepts an early return — before the agreed end date")
        void acceptsEarlyReturn() {
            Rental rental = validRental().actualReturnDate(START.plusDays(1)).build();
            assertThat(rental.isActualReturnDateValid()).isTrue();
            assertThat(validate(rental)).isEmpty();
        }

        @Test
        @DisplayName("accepts a late return — that is what finePerDay is for")
        void acceptsLateReturn() {
            Rental rental = validRental().actualReturnDate(END.plusDays(3)).build();
            assertThat(rental.isActualReturnDateValid()).isTrue();
            assertThat(validate(rental)).isEmpty();
        }

        @Test
        @DisplayName("rejects a return before the start date")
        void rejectsReturnBeforeStartDate() {
            Rental rental = validRental().actualReturnDate(START.minusDays(1)).build();
            assertThat(rental.isActualReturnDateValid()).isFalse();
            assertViolates(rental, "actualReturnDateValid");
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
            assertViolates(withMoney(property, new BigDecimal("-0.01")), property);
        }

        @ParameterizedTest(name = "{0} rejects null")
        @ValueSource(strings = {"dailyPrice", "depositAmount", "finePerDay", "totalPrice"})
        void rejectsNullMoney(String property) {
            assertViolates(withMoney(property, null), property);
        }

        @ParameterizedTest(name = "{0} accepts zero")
        @ValueSource(strings = {"dailyPrice", "depositAmount", "finePerDay", "totalPrice"})
        void acceptsZero(String property) {
            assertThat(validate(withMoney(property, BigDecimal.ZERO))).isEmpty();
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