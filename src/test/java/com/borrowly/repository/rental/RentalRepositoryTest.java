package com.borrowly.repository.rental;

import com.borrowly.model.item.Category;
import com.borrowly.model.item.Item;
import com.borrowly.model.item.ItemCondition;
import com.borrowly.model.rental.Rental;
import com.borrowly.model.rental.RentalStatus;
import com.borrowly.model.user.User;
import com.borrowly.support.AbstractPostgresTest;
import jakarta.persistence.EntityManager;
import org.hibernate.Session;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

// Liquibase owns the schema and the changesets are Postgres-specific, so there is no
// embedded database to swap in; AbstractPostgresTest starts a throwaway Postgres to run against.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.jpa.properties.hibernate.generate_statistics=true"
})
class RentalRepositoryTest extends AbstractPostgresTest {

    private static final LocalDate JUL_10 = LocalDate.of(2026, Month.JULY, 10);
    private static final LocalDate JUL_15 = LocalDate.of(2026, Month.JULY, 15);

    // Two more windows that do not overlap JUL_10-JUL_15. An item can only be out on one
    // live rental at a time (ex_rentals_no_overlap), so a second rental of the same item
    // has to sit in its own window.
    private static final LocalDate JUL_01 = LocalDate.of(2026, Month.JULY, 1);
    private static final LocalDate JUL_05 = LocalDate.of(2026, Month.JULY, 5);
    private static final LocalDate JUL_20 = LocalDate.of(2026, Month.JULY, 20);
    private static final LocalDate JUL_25 = LocalDate.of(2026, Month.JULY, 25);

    @Autowired
    private RentalRepository rentalRepository;

    @Autowired
    private EntityManager entityManager;

    private User owner;
    private User borrower;
    private User otherBorrower;
    private Category category;
    private Item item;

    private final Pageable firstPage = PageRequest.of(0, 10);

    @BeforeEach
    void seedBaseGraph() {
        owner = persistUser("owner@borrowly.test");
        borrower = persistUser("borrower@borrowly.test");
        otherBorrower = persistUser("other@borrowly.test");
        category = persistCategory("Power tools");
        item = persistItem(owner, "Bosch Drill");
        flushAndClear();
    }

    private User persistUser(String email) {
        User user = User.register("Test", "User", email, "hash");
        entityManager.persist(user);
        return user;
    }

    private Category persistCategory(String name) {
        Category created = Category.builder()
                .name(name)
                .build();
        entityManager.persist(created);
        return created;
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

    private Rental persistRental(Item rentedItem,
                                 User rentalBorrower,
                                 LocalDate startDate,
                                 LocalDate endDate,
                                 RentalStatus status) {
        Rental rental = Rental.builder()
                .item(rentedItem)
                .borrower(rentalBorrower)
                .startDate(startDate)
                .endDate(endDate)
                .itemTitle(rentedItem.getTitle())
                .dailyPrice(new BigDecimal("5.00"))
                .depositAmount(new BigDecimal("50.00"))
                .finePerDay(new BigDecimal("2.50"))
                .totalPrice(new BigDecimal("25.00"))
                .status(status)
                .build();
        entityManager.persist(rental);
        return rental;
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    private Statistics statistics() {
        return entityManager.unwrap(Session.class)
                .getSessionFactory()
                .getStatistics();
    }

    @Nested
    @DisplayName("findByBorrowerId")
    class FindByBorrower {

        @Test
        @DisplayName("returns only this borrower's rentals")
        void filtersByBorrower() {
            persistRental(item, borrower, JUL_10, JUL_15, RentalStatus.ACTIVE);
            persistRental(item, otherBorrower, JUL_20, JUL_25, RentalStatus.ACTIVE);
            flushAndClear();

            Page<Rental> page = rentalRepository.findByBorrowerId(borrower.getId(), firstPage);

            assertThat(page.getTotalElements()).isEqualTo(1);
            assertThat(page.getContent())
                    .singleElement()
                    .extracting(r -> r.getBorrower().getId())
                    .isEqualTo(borrower.getId());
        }

        @Test
        @DisplayName("returns an empty page for a borrower with no rentals")
        void emptyWhenNoRentals() {
            assertThat(rentalRepository.findByBorrowerId(borrower.getId(), firstPage))
                    .isEmpty();
        }
    }

    @Nested
    @DisplayName("findByBorrowerIdAndStatusIn")
    class FindByBorrowerAndStatus {

        @Test
        @DisplayName("keeps only rentals whose status is in the given set")
        void filtersByStatus() {
            persistRental(item, borrower, JUL_10, JUL_15, RentalStatus.ACTIVE);
            persistRental(item, borrower, JUL_20, JUL_25, RentalStatus.OVERDUE);
            // RETURNED may share the ACTIVE window - a finished rental frees the item.
            persistRental(item, borrower, JUL_10, JUL_15, RentalStatus.RETURNED);
            flushAndClear();

            Page<Rental> active = rentalRepository.findByBorrowerIdAndStatusIn(
                    borrower.getId(),
                    Set.of(RentalStatus.ACTIVE, RentalStatus.OVERDUE),
                    firstPage);

            assertThat(active.getTotalElements()).isEqualTo(2);
            assertThat(active.getContent())
                    .extracting(Rental::getStatus)
                    .containsExactlyInAnyOrder(RentalStatus.ACTIVE, RentalStatus.OVERDUE);
        }

        @Test
        @DisplayName("does not leak another borrower's rentals of the same status")
        void filtersByBorrowerAndStatus() {
            persistRental(item, otherBorrower, JUL_10, JUL_15, RentalStatus.ACTIVE);
            flushAndClear();

            assertThat(rentalRepository.findByBorrowerIdAndStatusIn(
                    borrower.getId(), Set.of(RentalStatus.ACTIVE), firstPage))
                    .isEmpty();
        }
    }

    @Nested
    @DisplayName("findByOwnerId")
    class FindByOwner {

        @Test
        @DisplayName("returns rentals of every item this user owns")
        void findsAcrossOwnedItems() {
            Item second = persistItem(owner, "Circular Saw");
            Item foreign = persistItem(otherBorrower, "Not Mine");

            persistRental(item, borrower, JUL_10, JUL_15, RentalStatus.ACTIVE);
            persistRental(second, borrower, JUL_10, JUL_15, RentalStatus.ACTIVE);
            persistRental(foreign, borrower, JUL_10, JUL_15, RentalStatus.ACTIVE);
            flushAndClear();

            Page<Rental> page = rentalRepository.findByOwnerId(owner.getId(), firstPage);

            assertThat(page.getTotalElements()).isEqualTo(2);
            assertThat(page.getContent())
                    .extracting(Rental::getItemTitle)
                    .containsExactlyInAnyOrder("Bosch Drill", "Circular Saw");
        }
    }

    @Nested
    @DisplayName("findByOwnerIdAndStatusIn")
    class FindByOwnerAndStatus {

        @Test
        @DisplayName("keeps only rentals whose status is in the given set")
        void filtersByStatus() {
            persistRental(item, borrower, JUL_10, JUL_15, RentalStatus.ACTIVE);
            persistRental(item, borrower, JUL_10, JUL_15, RentalStatus.RETURNED);
            flushAndClear();

            Page<Rental> page = rentalRepository.findByOwnerIdAndStatusIn(
                    owner.getId(), Set.of(RentalStatus.ACTIVE), firstPage);

            assertThat(page.getContent())
                    .singleElement()
                    .extracting(Rental::getStatus)
                    .isEqualTo(RentalStatus.ACTIVE);
        }
    }


    @Nested
    @DisplayName("existsOverlappingByStatuses")
    class OverlapGuard {

        private static final Set<RentalStatus> BLOCKING =
                Set.of(RentalStatus.ACTIVE, RentalStatus.OVERDUE);

        @BeforeEach
        void seedExistingRental() {
            persistRental(item, borrower, JUL_10, JUL_15, RentalStatus.ACTIVE);
            flushAndClear();
        }

        private boolean overlaps(LocalDate start, LocalDate end) {
            return rentalRepository.existsOverlappingByStatuses(
                    item.getId(), start, end, BLOCKING);
        }

        @Test
        @DisplayName("detects a window fully inside the existing rental")
        void containedWindowOverlaps() {
            assertThat(overlaps(LocalDate.of(2026, Month.JULY, 11), LocalDate.of(2026, Month.JULY, 14))).isTrue();
        }

        @Test
        @DisplayName("detects a window that straddles the existing rental")
        void straddlingWindowOverlaps() {
            assertThat(overlaps(LocalDate.of(2026, Month.JULY, 1), LocalDate.of(2026, Month.JULY, 31))).isTrue();
        }

        @Test
        @DisplayName("detects a window overlapping only the tail")
        void tailOverlaps() {
            assertThat(overlaps(LocalDate.of(2026, Month.JULY, 14), LocalDate.of(2026, Month.JULY, 20))).isTrue();
        }

        @Test
        @DisplayName("SAME-DAY BOUNDARY: a new rental starting the day the existing one ends overlaps")
        void sameDayStartOnExistingEndOverlaps() {
            assertThat(overlaps(JUL_15, LocalDate.of(2026, Month.JULY, 20))).isTrue();
        }

        @Test
        @DisplayName("SAME-DAY BOUNDARY: a new rental ending the day the existing one starts overlaps")
        void sameDayEndOnExistingStartOverlaps() {
            assertThat(overlaps(LocalDate.of(2026, Month.JULY, 5), JUL_10)).isTrue();
        }

        @Test
        @DisplayName("a window ending the day before the existing one starts does not overlap")
        void strictlyBeforeDoesNotOverlap() {
            assertThat(overlaps(LocalDate.of(2026, Month.JULY, 5), LocalDate.of(2026, Month.JULY, 9))).isFalse();
        }

        @Test
        @DisplayName("a window starting the day after the existing one ends does not overlap")
        void strictlyAfterDoesNotOverlap() {
            assertThat(overlaps(LocalDate.of(2026, Month.JULY, 16), LocalDate.of(2026, Month.JULY, 20))).isFalse();
        }

        @Test
        @DisplayName("a RETURNED rental no longer blocks the window")
        void returnedRentalDoesNotBlock() {
            Item free = persistItem(owner, "Free Item");
            persistRental(free, borrower, JUL_10, JUL_15, RentalStatus.RETURNED);
            flushAndClear();

            assertThat(rentalRepository.existsOverlappingByStatuses(
                    free.getId(), JUL_10, JUL_15, BLOCKING)).isFalse();
        }

        @Test
        @DisplayName("an OVERDUE rental still blocks — the item is physically still out")
        void overdueRentalBlocks() {
            Item held = persistItem(owner, "Still Out");
            persistRental(held, borrower, JUL_10, JUL_15, RentalStatus.OVERDUE);
            flushAndClear();

            assertThat(rentalRepository.existsOverlappingByStatuses(
                    held.getId(), JUL_10, JUL_15, BLOCKING)).isTrue();
        }

        @Test
        @DisplayName("overlap is scoped to a single item")
        void otherItemDoesNotCollide() {
            Item unrelated = persistItem(owner, "Unrelated");
            flushAndClear();

            assertThat(rentalRepository.existsOverlappingByStatuses(
                    unrelated.getId(), JUL_10, JUL_15, BLOCKING)).isFalse();
        }

        @Test
        @DisplayName("excluding-variant ignores the rental being edited")
        void excludingVariantIgnoresSelf() {
            Rental existing = rentalRepository
                    .findByOwnerId(owner.getId(), firstPage)
                    .getContent()
                    .getFirst();

            assertThat(rentalRepository.existsOverlappingByStatusesExcluding(
                    item.getId(), JUL_10, JUL_15, existing.getId(), BLOCKING)).isFalse();
        }
    }

    @Nested
    @DisplayName("@EntityGraph prevents N+1")
    class NPlusOne {

        @Test
        @DisplayName("3 active rentals across 2 items load in a constant number of queries")
        void ownerQueryIsConstantQueryCount() {
            Item second = persistItem(owner, "Circular Saw");

            persistRental(item, borrower, JUL_10, JUL_15, RentalStatus.ACTIVE);
            persistRental(item, otherBorrower, JUL_20, JUL_25, RentalStatus.ACTIVE);
            persistRental(second, borrower, JUL_10, JUL_15, RentalStatus.ACTIVE);
            flushAndClear();

            Statistics stats = statistics();
            stats.clear();

            Page<Rental> page = rentalRepository.findByOwnerIdAndStatusIn(
                    owner.getId(), Set.of(RentalStatus.ACTIVE), firstPage);

            page.getContent().forEach(rental -> {
                assertThat(rental.getItem().getTitle()).isNotNull();
                assertThat(rental.getItem().getOwner().getEmail()).isNotNull();
                assertThat(rental.getBorrower().getEmail()).isNotNull();
            });

            assertThat(page.getContent()).hasSize(3);

            assertThat(stats.getPrepareStatementCount())
                    .as("query count must not grow with the number of rentals")
                    .isLessThanOrEqualTo(2);
        }

        @Test
        @DisplayName("query count does not grow when the number of rentals grows")
        void queryCountIsIndependentOfRowCount() {
            Item second = persistItem(owner, "Circular Saw");
            persistRental(item, borrower, JUL_10, JUL_15, RentalStatus.ACTIVE);
            persistRental(item, otherBorrower, JUL_20, JUL_25, RentalStatus.ACTIVE);
            persistRental(second, borrower, JUL_10, JUL_15, RentalStatus.ACTIVE);
            flushAndClear();

            long baseline = countQueriesForOwnerFetch();

            Item third = persistItem(owner, "Extra Item");
            persistRental(third, borrower, JUL_10, JUL_15, RentalStatus.ACTIVE);
            persistRental(third, otherBorrower, JUL_20, JUL_25, RentalStatus.ACTIVE);
            persistRental(third, borrower, JUL_01, JUL_05, RentalStatus.ACTIVE);
            flushAndClear();

            long grown = countQueriesForOwnerFetch();

            assertThat(grown)
                    .as("doubling the rentals must not add queries — that would be N+1")
                    .isEqualTo(baseline);
        }

        private long countQueriesForOwnerFetch() {
            entityManager.clear();
            Statistics stats = statistics();
            stats.clear();

            Page<Rental> page = rentalRepository.findByOwnerIdAndStatusIn(
                    owner.getId(), Set.of(RentalStatus.ACTIVE), PageRequest.of(0, 50));

            page.getContent().forEach(rental -> {
                assertThat(rental.getItem().getOwner().getEmail()).isNotNull();
                assertThat(rental.getBorrower().getEmail()).isNotNull();
            });

            return stats.getPrepareStatementCount();
        }
    }
}