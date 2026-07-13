package com.borrowly.repository.rental;

import com.borrowly.model.item.Item;
import com.borrowly.model.rental.Rental;
import com.borrowly.model.rental.RentalStatus;
import com.borrowly.model.user.User;
import jakarta.persistence.EntityManager;
import org.hibernate.Session;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(properties = {
        "spring.jpa.properties.hibernate.generate_statistics=true"
})
class RentalRepositoryTest {

    private static final LocalDate JUL_10 = LocalDate.of(2026, Month.JULY, 10);
    private static final LocalDate JUL_15 = LocalDate.of(2026, Month.JULY, 15);

    @Autowired
    private RentalRepository rentalRepository;

    @Autowired
    private EntityManager entityManager;

    private User owner;
    private User borrower;
    private User otherBorrower;
    private Item item;

    private final Pageable firstPage = PageRequest.of(0, 10);

    @BeforeEach
    void seedBaseGraph() {
        owner = persistUser("owner@borrowly.test");
        borrower = persistUser("borrower@borrowly.test");
        otherBorrower = persistUser("other@borrowly.test");
        item = persistItem(owner, "Bosch Drill");
        flushAndClear();
    }

    private User persistUser(String email) {
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash("hash");
        user.setFirstName("Test");
        user.setLastName("User");
        entityManager.persist(user);
        return user;
    }

    private Item persistItem(User itemOwner, String title) {
        Item created = new Item();
        created.setOwner(itemOwner);
        created.setTitle(title);
        created.setPricePerDay(new BigDecimal("5.00"));
        created.setDepositAmount(new BigDecimal("50.00"));
        created.setFinePerDay(new BigDecimal("2.50"));
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
    @DisplayName("findByBorrower_Id")
    class FindByBorrower {

        @Test
        @DisplayName("returns only this borrower's rentals")
        void filtersByBorrower() {
            persistRental(item, borrower, JUL_10, JUL_15, RentalStatus.ACTIVE);
            persistRental(item, otherBorrower, JUL_10, JUL_15, RentalStatus.ACTIVE);
            flushAndClear();

            Page<Rental> page = rentalRepository.findByBorrower_Id(borrower.getId(), firstPage);

            assertThat(page.getTotalElements()).isEqualTo(1);
            assertThat(page.getContent())
                    .singleElement()
                    .extracting(r -> r.getBorrower().getId())
                    .isEqualTo(borrower.getId());
        }

        @Test
        @DisplayName("returns an empty page for a borrower with no rentals")
        void emptyWhenNoRentals() {
            assertThat(rentalRepository.findByBorrower_Id(borrower.getId(), firstPage))
                    .isEmpty();
        }
    }

    @Nested
    @DisplayName("findByBorrower_IdAndStatusIn")
    class FindByBorrowerAndStatus {

        @Test
        @DisplayName("keeps only rentals whose status is in the given set")
        void filtersByStatus() {
            persistRental(item, borrower, JUL_10, JUL_15, RentalStatus.ACTIVE);
            persistRental(item, borrower, JUL_10, JUL_15, RentalStatus.OVERDUE);
            persistRental(item, borrower, JUL_10, JUL_15, RentalStatus.RETURNED);
            flushAndClear();

            Page<Rental> active = rentalRepository.findByBorrower_IdAndStatusIn(
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

            assertThat(rentalRepository.findByBorrower_IdAndStatusIn(
                    borrower.getId(), Set.of(RentalStatus.ACTIVE), firstPage))
                    .isEmpty();
        }
    }

    @Nested
    @DisplayName("findByItem_Owner_Id")
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

            Page<Rental> page = rentalRepository.findByItem_Owner_Id(owner.getId(), firstPage);

            assertThat(page.getTotalElements()).isEqualTo(2);
            assertThat(page.getContent())
                    .extracting(Rental::getItemTitle)
                    .containsExactlyInAnyOrder("Bosch Drill", "Circular Saw");
        }
    }

    @Nested
    @DisplayName("findByItem_Owner_IdAndStatusIn")
    class FindByOwnerAndStatus {

        @Test
        @DisplayName("keeps only rentals whose status is in the given set")
        void filtersByStatus() {
            persistRental(item, borrower, JUL_10, JUL_15, RentalStatus.ACTIVE);
            persistRental(item, borrower, JUL_10, JUL_15, RentalStatus.RETURNED);
            flushAndClear();

            Page<Rental> page = rentalRepository.findByItem_Owner_IdAndStatusIn(
                    owner.getId(), Set.of(RentalStatus.ACTIVE), firstPage);

            assertThat(page.getContent())
                    .singleElement()
                    .extracting(Rental::getStatus)
                    .isEqualTo(RentalStatus.ACTIVE);
        }
    }

    @Nested
    @DisplayName("findByEndDateBeforeAndStatus — the overdue scan")
    class OverdueScan {

        @Test
        @DisplayName("finds ACTIVE rentals whose endDate is past, and ignores RETURNED ones")
        void findsOnlyPastDueActiveRentals() {
            LocalDate today = LocalDate.of(2026, Month.JULY, 20);

            Rental overdue = persistRental(item, borrower,
                    LocalDate.of(2026, Month.JULY, 1), LocalDate.of(2026, Month.JULY, 10), RentalStatus.ACTIVE);

            persistRental(item, borrower,
                    LocalDate.of(2026, Month.JULY, 1), LocalDate.of(2026, Month.JULY, 10), RentalStatus.RETURNED);

            persistRental(item, borrower,
                    LocalDate.of(2026, Month.JULY, 18), LocalDate.of(2026, Month.JULY, 25), RentalStatus.ACTIVE);

            flushAndClear();

            List<Rental> due = rentalRepository.findByEndDateBeforeAndStatus(
                    today, RentalStatus.ACTIVE);

            assertThat(due)
                    .singleElement()
                    .extracting(Rental::getId)
                    .isEqualTo(overdue.getId());
        }

        @Test
        @DisplayName("is exclusive on the cutoff — a rental ending exactly on the cutoff is not yet overdue")
        void cutoffIsExclusive() {
            LocalDate cutoff = LocalDate.of(2026, Month.JULY, 15);
            persistRental(item, borrower, JUL_10, cutoff, RentalStatus.ACTIVE);
            flushAndClear();

            assertThat(rentalRepository.findByEndDateBeforeAndStatus(cutoff, RentalStatus.ACTIVE))
                    .isEmpty();
        }

        @Test
        @DisplayName("returns a List so the scheduled job can walk the full result set")
        void returnsList() {
            persistRental(item, borrower, JUL_10, JUL_15, RentalStatus.ACTIVE);
            persistRental(item, otherBorrower, JUL_10, JUL_15, RentalStatus.ACTIVE);
            flushAndClear();

            List<Rental> due = rentalRepository.findByEndDateBeforeAndStatus(
                    LocalDate.of(2026, Month.JULY, 20), RentalStatus.ACTIVE);

            assertThat(due).hasSize(2);
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
                    .findByItem_Owner_Id(owner.getId(), firstPage)
                    .getContent()
                    .get(0);

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
            persistRental(item, otherBorrower, JUL_10, JUL_15, RentalStatus.ACTIVE);
            persistRental(second, borrower, JUL_10, JUL_15, RentalStatus.ACTIVE);
            flushAndClear();

            Statistics stats = statistics();
            stats.clear();

            Page<Rental> page = rentalRepository.findByItem_Owner_IdAndStatusIn(
                    owner.getId(), Set.of(RentalStatus.ACTIVE), firstPage);

            page.getContent().forEach(rental -> {
                rental.getItem().getTitle();
                rental.getItem().getOwner().getEmail();
                rental.getBorrower().getEmail();
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
            persistRental(item, otherBorrower, JUL_10, JUL_15, RentalStatus.ACTIVE);
            persistRental(second, borrower, JUL_10, JUL_15, RentalStatus.ACTIVE);
            flushAndClear();

            long baseline = countQueriesForOwnerFetch();

            Item third = persistItem(owner, "Extra Item");
            persistRental(third, borrower, JUL_10, JUL_15, RentalStatus.ACTIVE);
            persistRental(third, otherBorrower, JUL_10, JUL_15, RentalStatus.ACTIVE);
            persistRental(third, borrower, JUL_10, JUL_15, RentalStatus.ACTIVE);
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

            Page<Rental> page = rentalRepository.findByItem_Owner_IdAndStatusIn(
                    owner.getId(), Set.of(RentalStatus.ACTIVE), PageRequest.of(0, 50));

            page.getContent().forEach(rental -> {
                rental.getItem().getOwner().getEmail();
                rental.getBorrower().getEmail();
            });

            return stats.getPrepareStatementCount();
        }
    }
}