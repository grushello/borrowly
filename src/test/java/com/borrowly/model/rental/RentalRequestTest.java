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
import org.junit.jupiter.params.provider.EnumSource;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.Set;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

class RentalRequestTest {

    private static final LocalDate START = LocalDate.of(2026, Month.JANUARY, 1);
    private static final LocalDate END = LocalDate.of(2026, Month.JANUARY, 5);

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
                .pricePerDay(new BigDecimal("5.00"))
                .depositAmount(new BigDecimal("50.00"))
                .finePerDay(new BigDecimal("2.50"))
                .condition(ItemCondition.GOOD)
                .build();
    }

    private RentalRequest.RentalRequestBuilder validRequest() {
        return RentalRequest.builder()
                .item(item)
                .borrower(borrower)
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

    private RentalRequest requestIn(RentalRequestStatus terminal) {
        RentalRequest request = validRequest().build();
        switch (terminal) {
            case APPROVED -> request.approve();
            case REJECTED -> request.reject();
            case CANCELED -> request.cancel();
            case PENDING -> { /* already pending */ }
        }
        return request;
    }

    @Nested
    @DisplayName("builder")
    class BuilderTests {

        @Test
        @DisplayName("happy path — populates every field and validates cleanly")
        void buildsFully() {
            RentalRequest request = validRequest().build();

            assertThat(request.getId()).isNotNull();
            assertThat(request.getItem()).isSameAs(item);
            assertThat(request.getBorrower()).isSameAs(borrower);
            assertThat(request.getStartDate()).isEqualTo(START);
            assertThat(request.getEndDate()).isEqualTo(END);
            assertThat(request.getStatus()).isEqualTo(RentalRequestStatus.PENDING);
            assertThat(request.isPending()).isTrue();
            assertThat(validate(request)).isEmpty();
        }

        @Test
        @DisplayName("defaults status to PENDING when not set")
        void defaultsStatusToPending() {
            RentalRequest request = RentalRequest.builder()
                    .item(item)
                    .borrower(borrower)
                    .startDate(START)
                    .endDate(END)
                    .build();

            assertThat(request.getStatus()).isEqualTo(RentalRequestStatus.PENDING);
        }

        @Test
        @DisplayName("requestedAt is left to @CreationTimestamp")
        void requestedAtNotSetOnBuild() {
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
    @DisplayName("state cannot be set directly")
    class EncapsulationTests {

        @Test
        @DisplayName("there is no setStatus — status moves only through approve/reject/cancel")
        void statusHasNoSetter() {
            assertThat(RentalRequest.class.getMethods())
                    .as("a public setStatus would let a caller approve an "
                            + "already-rejected request")
                    .extracting(Method::getName)
                    .doesNotContain("setStatus");
        }

        @Test
        @DisplayName("the request's own terms have no setters either")
        void termsHaveNoSetters() {
            assertThat(RentalRequest.class.getMethods())
                    .extracting(Method::getName)
                    .doesNotContain(
                            "setStartDate", "setEndDate", "setItem", "setBorrower");
        }
    }

    @Nested
    @DisplayName("deciding a pending request")
    class TransitionTests {

        @Test
        @DisplayName("approve() moves PENDING -> APPROVED")
        void approvePending() {
            RentalRequest request = validRequest().build();

            request.approve();

            assertThat(request.getStatus()).isEqualTo(RentalRequestStatus.APPROVED);
            assertThat(request.isPending()).isFalse();
            assertThat(validate(request)).isEmpty();
        }

        @Test
        @DisplayName("reject() moves PENDING -> REJECTED")
        void rejectPending() {
            RentalRequest request = validRequest().build();

            request.reject();

            assertThat(request.getStatus()).isEqualTo(RentalRequestStatus.REJECTED);
            assertThat(request.isPending()).isFalse();
        }

        @Test
        @DisplayName("cancel() moves PENDING -> CANCELED")
        void cancelPending() {
            RentalRequest request = validRequest().build();

            request.cancel();

            assertThat(request.getStatus()).isEqualTo(RentalRequestStatus.CANCELED);
            assertThat(request.isPending()).isFalse();
        }
    }

    @Nested
    @DisplayName("a decided request cannot be decided again")
    class TerminalStateTests {

        @ParameterizedTest(name = "approve() rejects a {0} request")
        @EnumSource(value = RentalRequestStatus.class,
                names = {"APPROVED", "REJECTED", "CANCELED"})
        void cannotApproveDecided(RentalRequestStatus terminal) {
            assertRefuses(terminal, RentalRequest::approve, "approved");
        }

        @ParameterizedTest(name = "reject() rejects a {0} request")
        @EnumSource(value = RentalRequestStatus.class,
                names = {"APPROVED", "REJECTED", "CANCELED"})
        void cannotRejectDecided(RentalRequestStatus terminal) {
            assertRefuses(terminal, RentalRequest::reject, "rejected");
        }

        @ParameterizedTest(name = "cancel() rejects a {0} request")
        @EnumSource(value = RentalRequestStatus.class,
                names = {"APPROVED", "REJECTED", "CANCELED"})
        void cannotCancelDecided(RentalRequestStatus terminal) {
            assertRefuses(terminal, RentalRequest::cancel, "canceled");
        }

        private void assertRefuses(RentalRequestStatus terminal,
                                   Consumer<RentalRequest> transition,
                                   String verb) {
            RentalRequest request = requestIn(terminal);

            assertThatIllegalStateException()
                    .isThrownBy(() -> transition.accept(request))
                    .withMessageContaining("Only pending requests can be " + verb);

            assertThat(request.getStatus())
                    .as("the refused transition must not have changed the status")
                    .isEqualTo(terminal);
        }

        @Test
        @DisplayName("REJECTED -> APPROVED is refused — the case named in review")
        void rejectedCannotBeApproved() {
            RentalRequest request = validRequest().build();
            request.reject();

            assertThatIllegalStateException().isThrownBy(request::approve);

            assertThat(request.getStatus()).isEqualTo(RentalRequestStatus.REJECTED);
        }

        @Test
        @DisplayName("CANCELED -> APPROVED is refused — the other case named in review")
        void canceledCannotBeApproved() {
            RentalRequest request = validRequest().build();
            request.cancel();

            assertThatIllegalStateException().isThrownBy(request::approve);

            assertThat(request.getStatus()).isEqualTo(RentalRequestStatus.CANCELED);
        }

        @Test
        @DisplayName("approving twice is refused — would otherwise mint a second Rental")
        void cannotApproveTwice() {
            RentalRequest request = validRequest().build();
            request.approve();

            assertThatIllegalStateException().isThrownBy(request::approve);

            assertThat(request.getStatus()).isEqualTo(RentalRequestStatus.APPROVED);
        }
    }

    @Nested
    @DisplayName("@PrePersist")
    class PrePersistTests {

        @Test
        @DisplayName("sets status to PENDING if null")
        void defaultsNullStatusToPending() {
            RentalRequest request = validRequest().status(null).build();
            request.onCreate();
            assertThat(request.getStatus()).isEqualTo(RentalRequestStatus.PENDING);
        }

        @Test
        @DisplayName("does not overwrite an explicitly set status")
        void keepsExplicitStatus() {
            RentalRequest request = validRequest()
                    .status(RentalRequestStatus.APPROVED)
                    .build();
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
            RentalRequest request = validRequest().build();
            assertThat(request.isDateRangeValid()).isTrue();
            assertThat(validate(request)).isEmpty();
        }

        @Test
        @DisplayName("accepts endDate equal to startDate — an item can be rented for a single day")
        void acceptsSameDay() {
            RentalRequest request = validRequest().startDate(START).endDate(START).build();
            assertThat(request.isDateRangeValid()).isTrue();
            assertThat(validate(request)).isEmpty();
        }

        @Test
        @DisplayName("rejects endDate before startDate")
        void rejectsInvertedRange() {
            RentalRequest request = validRequest().startDate(END).endDate(START).build();
            assertThat(request.isDateRangeValid()).isFalse();
            assertViolates(request, "dateRangeValid");
        }

        @Test
        @DisplayName("a null startDate is reported by @NotNull alone — the range check stays quiet")
        void rejectsNullStartDate() {
            RentalRequest request = validRequest().startDate(null).build();

            assertThat(request.isDateRangeValid()).isTrue();
            assertThat(validate(request))
                    .extracting(v -> v.getPropertyPath().toString())
                    .containsExactly("startDate");
        }

        @Test
        @DisplayName("a null endDate is reported by @NotNull alone — the range check stays quiet")
        void rejectsNullEndDate() {
            RentalRequest request = validRequest().endDate(null).build();

            assertThat(request.isDateRangeValid()).isTrue();
            assertThat(validate(request))
                    .extracting(v -> v.getPropertyPath().toString())
                    .containsExactly("endDate");
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
}