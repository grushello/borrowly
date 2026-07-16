package com.borrowly.model.user;

import com.borrowly.model.item.Item;
import com.borrowly.model.item.ItemCondition;
import com.borrowly.model.rental.Rental;
import com.borrowly.model.rental.RentalStatus;
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

class ReviewTest {

    private static final LocalDate START = LocalDate.of(2026, Month.JULY, 1);
    private static final LocalDate END = LocalDate.of(2026, Month.JULY, 5);

    private static ValidatorFactory factory;
    private static Validator validator;

    private User reviewer;
    private Item item;
    private Rental rental;

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
        reviewer = User.register("Bo", "Borrower", "borrower@borrowly.test", "hash");

        item = Item.builder()
                .title("Bosch Drill")
                .description("Corded hammer drill")
                .pricePerDay(new BigDecimal("5.00"))
                .depositAmount(new BigDecimal("50.00"))
                .finePerDay(new BigDecimal("2.50"))
                .condition(ItemCondition.GOOD)
                .build();

        rental = Rental.builder()
                .item(item)
                .borrower(reviewer)
                .startDate(START)
                .endDate(END)
                .itemTitle("Bosch Drill")
                .dailyPrice(new BigDecimal("5.00"))
                .depositAmount(new BigDecimal("50.00"))
                .finePerDay(new BigDecimal("2.50"))
                .totalPrice(new BigDecimal("25.00"))
                .status(RentalStatus.RETURNED)
                .build();
    }

    private Review.ReviewBuilder validReview() {
        return Review.builder()
                .rating(4)
                .comment("Drill worked exactly as described, owner was easy to deal with.")
                .rental(rental)
                .reviewer(reviewer);
    }

    private Set<ConstraintViolation<Review>> validate(Review review) {
        return validator.validate(review);
    }

    private void assertViolates(Review review, String property) {
        assertThat(validate(review))
                .as("expected a violation on '%s'", property)
                .anySatisfy(v -> assertThat(v.getPropertyPath().toString()).isEqualTo(property));
    }

    @Nested
    @DisplayName("builder")
    class BuilderTests {

        @Test
        @DisplayName("populates every field it is given")
        void buildsFully() {
            Review review = validReview().build();

            assertThat(review.getId()).isNotNull();
            assertThat(review.getRating()).isEqualTo(4);
            assertThat(review.getComment())
                    .isEqualTo("Drill worked exactly as described, owner was easy to deal with.");
            assertThat(review.getRental()).isSameAs(rental);
            assertThat(review.getReviewer()).isSameAs(reviewer);
        }

        @Test
        @DisplayName("a fully populated Review has no violations")
        void validReviewPasses() {
            assertThat(validate(validReview().build())).isEmpty();
        }

        @Test
        @DisplayName("generates a distinct id per review")
        void generatesDistinctIds() {
            assertThat(validReview().build().getId())
                    .isNotEqualTo(validReview().build().getId());
        }

        @Test
        @DisplayName("the reviewed item is reached through the rental, not stored twice")
        void itemComesFromTheRental() {
            assertThat(validReview().build().getRental().getItem()).isSameAs(item);
        }

        @Test
        @DisplayName("toString does not touch the lazy reviewer/rental relations")
        void toStringExcludesRelations() {
            String s = validReview().build().toString();
            assertThat(s).contains("rating=4");
            assertThat(s).doesNotContain("reviewer=", "rental=");
        }
    }

    @Nested
    @DisplayName("rating")
    class RatingTests {

        @ParameterizedTest(name = "rating {0} is accepted")
        @ValueSource(ints = {1, 2, 3, 4, 5})
        void acceptsRatingsInRange(int rating) {
            assertThat(validate(validReview().rating(rating).build())).isEmpty();
        }

        @Test
        @DisplayName("rejects a rating of 0 — below the lower boundary")
        void rejectsZero() {
            assertViolates(validReview().rating(0).build(), "rating");
        }

        @Test
        @DisplayName("rejects a rating of 6 — above the upper boundary")
        void rejectsSix() {
            assertViolates(validReview().rating(6).build(), "rating");
        }

        @ParameterizedTest(name = "rating {0} is rejected")
        @ValueSource(ints = {-1, 0, 6, 100})
        void rejectsRatingsOutOfRange(int rating) {
            assertViolates(validReview().rating(rating).build(), "rating");
        }

        @Test
        @DisplayName("rejects a null rating — a review must carry a score")
        void rejectsNull() {
            assertViolates(validReview().rating(null).build(), "rating");
        }
    }

    @Nested
    @DisplayName("comment")
    class CommentTests {

        @Test
        @DisplayName("is optional — a rating-only review is valid")
        void acceptsNull() {
            Review review = validReview().comment(null).build();

            assertThat(review.getComment()).isNull();
            assertThat(validate(review)).isEmpty();
        }

        @Test
        @DisplayName("accepts a comment of exactly 2000 characters")
        void acceptsBoundary() {
            assertThat(validate(validReview().comment("x".repeat(2000)).build())).isEmpty();
        }

        @Test
        @DisplayName("rejects a comment longer than 2000 characters")
        void rejectsTooLong() {
            assertViolates(validReview().comment("x".repeat(2001)).build(), "comment");
        }
    }

    @Nested
    @DisplayName("relations")
    class RelationTests {

        @Test
        @DisplayName("rejects a null rental — you can only review what you rented")
        void rejectsNullRental() {
            assertViolates(validReview().rental(null).build(), "rental");
        }

        @Test
        @DisplayName("rejects a null reviewer — a review must belong to someone")
        void rejectsNullReviewer() {
            assertViolates(validReview().reviewer(null).build(), "reviewer");
        }
    }

    @Nested
    @DisplayName("immutability")
    class ImmutabilityTests {

        @Test
        @DisplayName("a posted review has no setters — it cannot be silently rewritten")
        void hasNoSetters() {
            assertThat(Review.class.getMethods())
                    .extracting(Method::getName)
                    .noneMatch(name -> name.startsWith("set"));
        }
    }
}