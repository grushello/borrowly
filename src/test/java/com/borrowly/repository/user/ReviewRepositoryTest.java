package com.borrowly.repository.user;

import com.borrowly.model.item.Item;
import com.borrowly.model.item.ItemCondition;
import com.borrowly.model.rental.Rental;
import com.borrowly.model.rental.RentalStatus;
import com.borrowly.model.user.Review;
import com.borrowly.model.user.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ReviewRepositoryTest {

    private static final LocalDate JUL_10 = LocalDate.of(2026, Month.JULY, 10);
    private static final LocalDate JUL_15 = LocalDate.of(2026, Month.JULY, 15);

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private EntityManager entityManager;

    private User owner;
    private User borrower;
    private User otherBorrower;
    private Item item;
    private Item otherItem;

    private final Pageable firstPage = PageRequest.of(0, 10);

    @BeforeEach
    void seedBaseGraph() {
        owner = persistUser("owner@borrowly.test");
        borrower = persistUser("borrower@borrowly.test");
        otherBorrower = persistUser("other@borrowly.test");
        item = persistItem(owner, "Bosch Drill");
        otherItem = persistItem(owner, "Circular Saw");
        flushAndClear();
    }

    private User persistUser(String email) {
        User user = User.register("Test", "User", email, "hash");
        entityManager.persist(user);
        return user;
    }

    private Item persistItem(User itemOwner, String title) {
        Item created = Item.builder()
                .owner(itemOwner)
                .title(title)
                .pricePerDay(new BigDecimal("5.00"))
                .depositAmount(new BigDecimal("50.00"))
                .finePerDay(new BigDecimal("2.50"))
                .condition(ItemCondition.GOOD)
                .build();
        entityManager.persist(created);
        return created;
    }

    private Rental persistRental(Item rentedItem, User rentalBorrower) {
        Rental rental = Rental.builder()
                .item(rentedItem)
                .borrower(rentalBorrower)
                .startDate(JUL_10)
                .endDate(JUL_15)
                .itemTitle(rentedItem.getTitle())
                .dailyPrice(new BigDecimal("5.00"))
                .depositAmount(new BigDecimal("50.00"))
                .finePerDay(new BigDecimal("2.50"))
                .totalPrice(new BigDecimal("25.00"))
                .status(RentalStatus.RETURNED)
                .build();
        entityManager.persist(rental);
        return rental;
    }

    private Review persistReview(Rental rental, User reviewer, int rating) {
        Review review = Review.builder()
                .rental(rental)
                .reviewer(reviewer)
                .rating(rating)
                .comment("Rated " + rating)
                .build();
        entityManager.persist(review);
        return review;
    }

    private Review persistReviewFor(Item reviewedItem, User reviewer, int rating) {
        return persistReview(persistRental(reviewedItem, reviewer), reviewer, rating);
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    @Nested
    @DisplayName("findByRental_Item_IdOrderByCreatedAtDesc")
    class FindByItem {

        @Test
        @DisplayName("returns only the reviews of the requested item")
        void filtersByItem() {
            persistReviewFor(item, borrower, 5);
            persistReviewFor(otherItem, borrower, 1);
            flushAndClear();

            Page<Review> page = reviewRepository
                    .findByRental_Item_IdOrderByCreatedAtDesc(item.getId(), firstPage);

            assertThat(page.getTotalElements()).isEqualTo(1);
            assertThat(page.getContent())
                    .singleElement()
                    .extracting(r -> r.getRental().getItem().getId())
                    .isEqualTo(item.getId());
        }

        @Test
        @DisplayName("returns the newest review first")
        void ordersByCreatedAtDesc() {
            persistReviewFor(item, borrower, 3);
            persistReviewFor(item, otherBorrower, 4);
            persistReviewFor(item, owner, 5);
            flushAndClear();

            List<Review> reviews = reviewRepository
                    .findByRental_Item_IdOrderByCreatedAtDesc(item.getId(), firstPage)
                    .getContent();

            assertThat(reviews).hasSize(3);
            assertThat(reviews)
                    .extracting(Review::getCreatedAt)
                    .isSortedAccordingTo(java.util.Comparator.reverseOrder());
        }

        @Test
        @DisplayName("returns an empty page for an item with no reviews")
        void emptyWhenNoReviews() {
            assertThat(reviewRepository
                    .findByRental_Item_IdOrderByCreatedAtDesc(item.getId(), firstPage))
                    .isEmpty();
        }

        @Test
        @DisplayName("paginates")
        void paginates() {
            persistReviewFor(item, borrower, 1);
            persistReviewFor(item, otherBorrower, 2);
            persistReviewFor(item, owner, 3);
            flushAndClear();

            Page<Review> page = reviewRepository
                    .findByRental_Item_IdOrderByCreatedAtDesc(item.getId(), PageRequest.of(0, 2));

            assertThat(page.getContent()).hasSize(2);
            assertThat(page.getTotalElements()).isEqualTo(3);
            assertThat(page.getTotalPages()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("findByReviewer_Id")
    class FindByReviewer {

        @Test
        @DisplayName("returns only this reviewer's reviews")
        void filtersByReviewer() {
            persistReviewFor(item, borrower, 5);
            persistReviewFor(item, otherBorrower, 2);
            flushAndClear();

            Page<Review> page = reviewRepository.findByReviewer_Id(borrower.getId(), firstPage);

            assertThat(page.getTotalElements()).isEqualTo(1);
            assertThat(page.getContent())
                    .singleElement()
                    .extracting(r -> r.getReviewer().getId())
                    .isEqualTo(borrower.getId());
        }

        @Test
        @DisplayName("spans every item the reviewer has rented")
        void spansItems() {
            persistReviewFor(item, borrower, 5);
            persistReviewFor(otherItem, borrower, 4);
            flushAndClear();

            assertThat(reviewRepository.findByReviewer_Id(borrower.getId(), firstPage))
                    .hasSize(2);
        }

        @Test
        @DisplayName("returns an empty page for a reviewer with no reviews")
        void emptyWhenNoReviews() {
            assertThat(reviewRepository.findByReviewer_Id(borrower.getId(), firstPage)).isEmpty();
        }
    }

    @Nested
    @DisplayName("countByRental_Item_Id")
    class CountByItem {

        @Test
        @DisplayName("counts only the requested item's reviews")
        void countsPerItem() {
            persistReviewFor(item, borrower, 5);
            persistReviewFor(item, otherBorrower, 3);
            persistReviewFor(otherItem, borrower, 1);
            flushAndClear();

            assertThat(reviewRepository.countByRental_Item_Id(item.getId())).isEqualTo(2);
            assertThat(reviewRepository.countByRental_Item_Id(otherItem.getId())).isEqualTo(1);
        }

        @Test
        @DisplayName("returns zero for an item with no reviews")
        void zeroWhenNoReviews() {
            assertThat(reviewRepository.countByRental_Item_Id(item.getId())).isZero();
        }
    }

    @Nested
    @DisplayName("averageRatingByItemId")
    class AverageRating {

        @Test
        @DisplayName("returns null with 0 reviews — AVG over an empty set is NULL, not 0")
        void nullWhenNoReviews() {
            assertThat(reviewRepository.averageRatingByItemId(item.getId()))
                    .as("the service is responsible for turning this into 0.0 / Optional")
                    .isNull();
        }

        @Test
        @DisplayName("returns the rating itself with 1 review")
        void singleReview() {
            persistReviewFor(item, borrower, 4);
            flushAndClear();

            assertThat(reviewRepository.averageRatingByItemId(item.getId()))
                    .isEqualTo(4.0d);
        }

        @Test
        @DisplayName("averages 3 reviews (2 + 4 + 5) / 3")
        void threeReviews() {
            persistReviewFor(item, borrower, 2);
            persistReviewFor(item, otherBorrower, 4);
            persistReviewFor(item, owner, 5);
            flushAndClear();

            assertThat(reviewRepository.averageRatingByItemId(item.getId()))
                    .isCloseTo(11.0d / 3.0d, org.assertj.core.data.Offset.offset(0.0001d));
        }

        @Test
        @DisplayName("does not average in another item's reviews")
        void scopedToItem() {
            persistReviewFor(item, borrower, 5);
            persistReviewFor(otherItem, borrower, 1);
            flushAndClear();

            assertThat(reviewRepository.averageRatingByItemId(item.getId())).isEqualTo(5.0d);
            assertThat(reviewRepository.averageRatingByItemId(otherItem.getId())).isEqualTo(1.0d);
        }

        @Test
        @DisplayName("returns null for an item that does not exist")
        void nullForUnknownItem() {
            assertThat(reviewRepository.averageRatingByItemId(UUID.randomUUID())).isNull();
        }
    }

    @Nested
    @DisplayName("existsByRental_Id")
    class ExistsByRental {

        @Test
        @DisplayName("true once the rental has been reviewed")
        void trueWhenReviewed() {
            Rental rental = persistRental(item, borrower);
            persistReview(rental, borrower, 5);
            flushAndClear();

            assertThat(reviewRepository.existsByRental_Id(rental.getId())).isTrue();
        }

        @Test
        @DisplayName("false for a rental that has not been reviewed")
        void falseWhenNotReviewed() {
            Rental rental = persistRental(item, borrower);
            flushAndClear();

            assertThat(reviewRepository.existsByRental_Id(rental.getId())).isFalse();
        }

        @Test
        @DisplayName("false for a rental that does not exist")
        void falseForUnknownRental() {
            assertThat(reviewRepository.existsByRental_Id(UUID.randomUUID())).isFalse();
        }

        @Test
        @DisplayName("is the clean pre-check for the uk_reviews_rental constraint")
        void guardsTheUniqueConstraint() {
            Rental rental = persistRental(item, borrower);
            persistReview(rental, borrower, 5);
            flushAndClear();

            assertThat(reviewRepository.existsByRental_Id(rental.getId()))
                    .as("the service checks this instead of catching a constraint violation")
                    .isTrue();

            Rental managed = entityManager.find(Rental.class, rental.getId());
            reviewRepository.save(Review.builder()
                    .rental(managed)
                    .reviewer(entityManager.find(User.class, borrower.getId()))
                    .rating(1)
                    .comment("second review of the same rental")
                    .build());

            assertThatExceptionOfType(DataIntegrityViolationException.class)
                    .as("one review per rental is enforced by the DB as the last line of defence")
                    .isThrownBy(() -> entityManager.flush());
        }
    }
}