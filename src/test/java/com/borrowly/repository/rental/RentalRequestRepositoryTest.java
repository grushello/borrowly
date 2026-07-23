package com.borrowly.repository.rental;

import com.borrowly.model.item.Category;
import com.borrowly.model.item.Item;
import com.borrowly.model.item.ItemCondition;
import com.borrowly.model.rental.RentalRequest;
import com.borrowly.model.rental.RentalRequestStatus;
import com.borrowly.model.user.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import com.borrowly.support.AbstractPostgresTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RentalRequestRepositoryTest extends AbstractPostgresTest {

    private static final LocalDate JUL_10 = LocalDate.of(2026, Month.JULY, 10);
    private static final LocalDate JUL_15 = LocalDate.of(2026, Month.JULY, 15);

    @Autowired
    private RentalRequestRepository rentalRequestRepository;

    @Autowired
    private EntityManager entityManager;

    private User owner;
    private User otherOwner;
    private User borrower;
    private User otherBorrower;
    private Item item;
    private Item otherItem;
    private Item foreignItem;
    private Category category;

    private final Pageable firstPage = PageRequest.of(0, 10);

    @BeforeEach
    void seedBaseGraph() {
        owner = persistUser("owner@borrowly.test");
        otherOwner = persistUser("other-owner@borrowly.test");
        borrower = persistUser("borrower@borrowly.test");
        otherBorrower = persistUser("other-borrower@borrowly.test");
        category = persistCategory("Power tools");

        item = persistItem(owner, "Bosch Drill");
        otherItem = persistItem(owner, "Circular Saw");
        foreignItem = persistItem(otherOwner, "Pressure Washer");

        flushAndClear();
    }

    private Category persistCategory(String name) {
        Category created = Category.builder()
                .name(name)
                .build();
        entityManager.persist(created);
        return created;
    }

    private User persistUser(String email) {
        User user = User.register("Test", "User", email, "hash");
        entityManager.persist(user);
        return user;
    }

    private Item persistItem(User itemOwner, String title) {
        Item created = Item.builder()
                .owner(itemOwner)
                .category(category)
                .title(title)
                .pricePerDay(new BigDecimal("5.00"))
                .depositAmount(new BigDecimal("50.00"))
                .finePerDay(new BigDecimal("2.50"))
                .condition(ItemCondition.GOOD)
                .build();
        entityManager.persist(created);
        return created;
    }

    private void persistRequest(Item requestedItem,
                                User requestBorrower,
                                LocalDate startDate,
                                LocalDate endDate,
                                RentalRequestStatus status) {
        RentalRequest request = RentalRequest.builder()
                .item(requestedItem)
                .borrower(requestBorrower)
                .startDate(startDate)
                .endDate(endDate)
                .status(status)
                .build();
        entityManager.persist(request);
    }

    private void persistRequest(Item requestedItem,
                                User requestBorrower,
                                RentalRequestStatus status) {
        persistRequest(requestedItem, requestBorrower, JUL_10, JUL_15, status);
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    @Nested
    @DisplayName("findByBorrowerId")
    class FindByBorrower {

        @Test
        @DisplayName("returns only this borrower's outgoing requests")
        void filtersByBorrower() {
            persistRequest(item, borrower, RentalRequestStatus.PENDING);
            persistRequest(item, otherBorrower, RentalRequestStatus.PENDING);
            flushAndClear();

            Page<RentalRequest> page =
                    rentalRequestRepository.findByBorrowerId(borrower.getId(), firstPage);

            assertThat(page.getTotalElements()).isEqualTo(1);
            assertThat(page.getContent())
                    .singleElement()
                    .extracting(rr -> rr.getBorrower().getId())
                    .isEqualTo(borrower.getId());
        }

        @Test
        @DisplayName("spans every item and owner the borrower has requested from")
        void spansItems() {
            persistRequest(item, borrower, RentalRequestStatus.PENDING);
            persistRequest(foreignItem, borrower, RentalRequestStatus.APPROVED);
            flushAndClear();

            assertThat(rentalRequestRepository.findByBorrowerId(borrower.getId(), firstPage))
                    .hasSize(2);
        }

        @Test
        @DisplayName("includes requests of every status — outgoing history, not just pending")
        void includesAllStatuses() {
            persistRequest(item, borrower, RentalRequestStatus.PENDING);
            persistRequest(otherItem, borrower, RentalRequestStatus.REJECTED);
            flushAndClear();

            assertThat(rentalRequestRepository.findByBorrowerId(borrower.getId(), firstPage))
                    .extracting(RentalRequest::getStatus)
                    .containsExactlyInAnyOrder(
                            RentalRequestStatus.PENDING, RentalRequestStatus.REJECTED);
        }

        @Test
        @DisplayName("returns an empty page for a borrower with no requests")
        void emptyWhenNone() {
            assertThat(rentalRequestRepository.findByBorrowerId(borrower.getId(), firstPage))
                    .isEmpty();
        }
    }

    @Nested
    @DisplayName("findByOwnerId")
    class FindByOwner {

        @Test
        @DisplayName("returns requests for every item this lender owns, and nobody else's")
        void filtersByOwner() {
            persistRequest(item, borrower, RentalRequestStatus.PENDING);
            persistRequest(otherItem, otherBorrower, RentalRequestStatus.PENDING);
            persistRequest(foreignItem, borrower, RentalRequestStatus.PENDING);
            flushAndClear();

            Page<RentalRequest> page =
                    rentalRequestRepository.findByOwnerId(owner.getId(), firstPage);

            assertThat(page.getTotalElements())
                    .as("the request on otherOwner's item must not leak in")
                    .isEqualTo(2);
            assertThat(page.getContent())
                    .extracting(rr -> rr.getItem().getOwner().getId())
                    .containsOnly(owner.getId());
        }

        @Test
        @DisplayName("returns an empty page for a lender with no incoming requests")
        void emptyWhenNone() {
            assertThat(rentalRequestRepository.findByOwnerId(owner.getId(), firstPage))
                    .isEmpty();
        }
    }

    @Nested
    @DisplayName("findByOwnerIdAndStatus")
    class FindByOwnerAndStatus {

        @Test
        @DisplayName("narrows the lender's inbox to a single status")
        void filtersByStatus() {
            persistRequest(item, borrower, RentalRequestStatus.PENDING);
            persistRequest(otherItem, otherBorrower, RentalRequestStatus.APPROVED);
            persistRequest(otherItem, borrower, RentalRequestStatus.REJECTED);
            flushAndClear();

            Page<RentalRequest> pending = rentalRequestRepository.findByOwnerIdAndStatus(
                    owner.getId(), RentalRequestStatus.PENDING, firstPage);

            assertThat(pending.getTotalElements()).isEqualTo(1);
            assertThat(pending.getContent())
                    .singleElement()
                    .extracting(RentalRequest::getStatus)
                    .isEqualTo(RentalRequestStatus.PENDING);
        }

        @Test
        @DisplayName("still respects the owner boundary")
        void staysScopedToOwner() {
            persistRequest(foreignItem, borrower, RentalRequestStatus.PENDING);
            flushAndClear();

            assertThat(rentalRequestRepository.findByOwnerIdAndStatus(
                    owner.getId(), RentalRequestStatus.PENDING, firstPage))
                    .as("a PENDING request on someone else's item is not this lender's inbox")
                    .isEmpty();
        }

        @Test
        @DisplayName("returns an empty page when no request has that status")
        void emptyWhenNoneMatch() {
            persistRequest(item, borrower, RentalRequestStatus.PENDING);
            flushAndClear();

            assertThat(rentalRequestRepository.findByOwnerIdAndStatus(
                    owner.getId(), RentalRequestStatus.CANCELED, firstPage))
                    .isEmpty();
        }
    }

    @Nested
    @DisplayName("existsByItemIdAndBorrowerIdAndStatusIn")
    class ExistsByItemAndBorrower {

        private final Set<RentalRequestStatus> blocking =
                Set.of(RentalRequestStatus.PENDING, RentalRequestStatus.APPROVED);

        @Test
        @DisplayName("true when this borrower already has a PENDING request on this item")
        void trueForPending() {
            persistRequest(item, borrower, RentalRequestStatus.PENDING);
            flushAndClear();

            assertThat(rentalRequestRepository.existsByItemIdAndBorrowerIdAndStatusIn(
                    item.getId(), borrower.getId(), blocking)).isTrue();
        }

        @Test
        @DisplayName("false when the borrower's only request was rejected — they may ask again")
        void falseForRejected() {
            persistRequest(item, borrower, RentalRequestStatus.REJECTED);
            flushAndClear();

            assertThat(rentalRequestRepository.existsByItemIdAndBorrowerIdAndStatusIn(
                    item.getId(), borrower.getId(), blocking))
                    .as("REJECTED is not in the blocking set")
                    .isFalse();
        }

        @Test
        @DisplayName("false when the PENDING request belongs to a different borrower")
        void scopedToBorrower() {
            persistRequest(item, otherBorrower, RentalRequestStatus.PENDING);
            flushAndClear();

            assertThat(rentalRequestRepository.existsByItemIdAndBorrowerIdAndStatusIn(
                    item.getId(), borrower.getId(), blocking)).isFalse();
        }

        @Test
        @DisplayName("false when the borrower's PENDING request is on a different item")
        void scopedToItem() {
            persistRequest(otherItem, borrower, RentalRequestStatus.PENDING);
            flushAndClear();

            assertThat(rentalRequestRepository.existsByItemIdAndBorrowerIdAndStatusIn(
                    item.getId(), borrower.getId(), blocking)).isFalse();
        }

        @Test
        @DisplayName("false when nothing has been requested at all")
        void falseWhenEmpty() {
            assertThat(rentalRequestRepository.existsByItemIdAndBorrowerIdAndStatusIn(
                    item.getId(), borrower.getId(), blocking)).isFalse();
        }
    }

    @Nested
    @DisplayName("existsOverlappingApproved")
    class ExistsOverlappingApproved {

        private void bookJul10To15() {
            persistRequest(item, otherBorrower, JUL_10, JUL_15, RentalRequestStatus.APPROVED);
            flushAndClear();
        }

        private boolean overlaps(LocalDate start, LocalDate end) {
            return rentalRequestRepository.existsOverlappingApproved(item.getId(), start, end);
        }

        @Test
        @DisplayName("true for a range that overlaps the booked window")
        void trueForOverlap() {
            bookJul10To15();

            assertThat(overlaps(JUL_10.plusDays(2), JUL_15.plusDays(2)))
                    .as("Jul 12 -> Jul 17 runs into the Jul 10 -> Jul 15 booking")
                    .isTrue();
        }

        @Test
        @DisplayName("false for a range entirely after the booked window")
        void falseForNonOverlap() {
            bookJul10To15();

            assertThat(overlaps(JUL_15.plusDays(1), JUL_15.plusDays(5)))
                    .as("Jul 16 -> Jul 20 starts the day the booking ends + 1")
                    .isFalse();
        }

        @Test
        @DisplayName("false for a range entirely before the booked window")
        void falseForRangeBefore() {
            bookJul10To15();

            assertThat(overlaps(JUL_10.minusDays(5), JUL_10.minusDays(1))).isFalse();
        }

        @Test
        @DisplayName("true when the new range merely touches the last booked day")
        void trueOnBoundary() {
            bookJul10To15();

            assertThat(overlaps(JUL_15, JUL_15.plusDays(3)))
                    .as("the item is still out on Jul 15 — this is the off-by-one that "
                            + "double-books in production")
                    .isTrue();
        }

        @Test
        @DisplayName("true when the new range fully contains the booked window")
        void trueWhenEnclosing() {
            bookJul10To15();

            assertThat(overlaps(JUL_10.minusDays(3), JUL_15.plusDays(3))).isTrue();
        }

        @Test
        @DisplayName("true when the new range sits entirely inside the booked window")
        void trueWhenEnclosed() {
            bookJul10To15();

            assertThat(overlaps(JUL_10.plusDays(1), JUL_15.minusDays(1))).isTrue();
        }

        @Test
        @DisplayName("ignores a PENDING request — only APPROVED holds the item")
        void ignoresPending() {
            persistRequest(item, otherBorrower, JUL_10, JUL_15, RentalRequestStatus.PENDING);
            flushAndClear();

            assertThat(overlaps(JUL_10, JUL_15))
                    .as("a pending request has not been granted, so it books nothing")
                    .isFalse();
        }

        @Test
        @DisplayName("ignores REJECTED and CANCELED requests")
        void ignoresDeadStatuses() {
            persistRequest(item, otherBorrower, JUL_10, JUL_15, RentalRequestStatus.REJECTED);
            persistRequest(item, borrower, JUL_10, JUL_15, RentalRequestStatus.CANCELED);
            flushAndClear();

            assertThat(overlaps(JUL_10, JUL_15)).isFalse();
        }

        @Test
        @DisplayName("is scoped to the item — another item's booking does not block this one")
        void scopedToItem() {
            persistRequest(otherItem, otherBorrower, JUL_10, JUL_15, RentalRequestStatus.APPROVED);
            flushAndClear();

            assertThat(overlaps(JUL_10, JUL_15)).isFalse();
        }

        @Test
        @DisplayName("false for an item that does not exist")
        void falseForUnknownItem() {
            bookJul10To15();

            assertThat(rentalRequestRepository.existsOverlappingApproved(
                    UUID.randomUUID(), JUL_10, JUL_15)).isFalse();
        }
    }
}