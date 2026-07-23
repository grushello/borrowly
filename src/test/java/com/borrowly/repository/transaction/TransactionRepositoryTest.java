package com.borrowly.repository.transaction;

import com.borrowly.model.item.Category;
import com.borrowly.model.item.Item;
import com.borrowly.model.item.ItemCondition;
import com.borrowly.model.rental.Rental;
import com.borrowly.model.rental.RentalStatus;
import com.borrowly.model.transaction.Transaction;
import com.borrowly.model.transaction.TransactionStatus;
import com.borrowly.model.transaction.TransactionType;
import com.borrowly.model.user.User;
import jakarta.persistence.EntityManager;
import org.hibernate.Session;
import org.hibernate.stat.Statistics;
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
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

// Liquibase owns the schema and the changesets are Postgres-specific, so there is no
// embedded database to swap in; these run against the same Postgres the app uses.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.jpa.properties.hibernate.generate_statistics=true"
})
class TransactionRepositoryTest extends AbstractPostgresTest {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private EntityManager entityManager;

    private User user;
    private User otherUser;
    private User owner;
    private Category category;
    private Item item;
    private Rental rental;

    private final Pageable firstPage = PageRequest.of(0, 10);

    @BeforeEach
    void seedBaseGraph() {
        user = persistUser("user@borrowly.test");
        otherUser = persistUser("other@borrowly.test");
        owner = persistUser("owner@borrowly.test");
        category = persistCategory("Power tools");
        item = persistItem(owner, "Bosch Drill");
        rental = persistRental(item, user);

        flushAndClear();
    }

    private User persistUser(String email) {
        User created = User.register("Test", "User", email, "hash");
        entityManager.persist(created);
        return created;
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

    private Rental persistRental(Item rentedItem, User borrower) {
        Rental created = Rental.builder()
                .item(rentedItem)
                .borrower(borrower)
                .startDate(LocalDate.of(2026, Month.JULY, 10))
                .endDate(LocalDate.of(2026, Month.JULY, 15))
                .itemTitle(rentedItem.getTitle())
                .dailyPrice(new BigDecimal("5.00"))
                .depositAmount(new BigDecimal("50.00"))
                .finePerDay(new BigDecimal("2.50"))
                .totalPrice(new BigDecimal("25.00"))
                .status(RentalStatus.ACTIVE)
                .build();
        entityManager.persist(created);
        return created;
    }

    private Transaction persistTransaction(User owningUser, TransactionType type, Rental linkedRental) {
        Transaction created = Transaction.builder()
                .user(owningUser)
                .rental(linkedRental)
                .type(type)
                .status(TransactionStatus.COMPLETED)
                .amount(new BigDecimal("10.00"))
                .build();
        entityManager.persist(created);
        return created;
    }

    private Transaction persistTransaction(User owningUser, TransactionType type) {
        return persistTransaction(owningUser, type, null);
    }

    // createdAt is @CreationTimestamp and updatable = false, so neither entity
    // dirty-checking nor a JPQL bulk update can change it once persisted; a native
    // UPDATE is the only way to control ordering/range boundaries in these tests.
    private void forceCreatedAt(Transaction transaction, LocalDateTime createdAt) {
        entityManager.createNativeQuery("update transactions set created_at = ?1 where id = ?2")
                .setParameter(1, createdAt)
                .setParameter(2, transaction.getId())
                .executeUpdate();
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
    @DisplayName("findByUserIdOrderByCreatedAtDesc")
    class FindByUserOrderByCreatedAtDesc {

        @Test
        @DisplayName("returns only this user's transactions, newest first")
        void filtersByUserAndOrdersDescending() {
            Transaction oldest = persistTransaction(user, TransactionType.TOP_UP, rental);
            Transaction newest = persistTransaction(user, TransactionType.RENT_PAYMENT, rental);
            persistTransaction(otherUser, TransactionType.TOP_UP);
            flushAndClear();

            forceCreatedAt(oldest, LocalDateTime.of(2026, Month.JULY, 1, 10, 0));
            forceCreatedAt(newest, LocalDateTime.of(2026, Month.JULY, 5, 10, 0));

            Page<Transaction> page = transactionRepository
                    .findByUserIdOrderByCreatedAtDesc(user.getId(), firstPage);

            assertThat(page.getTotalElements()).isEqualTo(2);
            assertThat(page.getContent())
                    .extracting(Transaction::getId)
                    .containsExactly(newest.getId(), oldest.getId());
        }

        @Test
        @DisplayName("returns an empty page for a user with no transactions")
        void emptyWhenNoTransactions() {
            assertThat(transactionRepository
                    .findByUserIdOrderByCreatedAtDesc(otherUser.getId(), firstPage))
                    .isEmpty();
        }
    }

    @Nested
    @DisplayName("findByUserIdAndTypeIn")
    class FindByUserAndTypeIn {

        @Test
        @DisplayName("keeps only transactions whose type is in the given set")
        void filtersByType() {
            persistTransaction(user, TransactionType.TOP_UP);
            persistTransaction(user, TransactionType.RENT_PAYMENT);
            persistTransaction(user, TransactionType.WITHDRAWAL);
            flushAndClear();

            Page<Transaction> page = transactionRepository.findByUserIdAndTypeIn(
                    user.getId(),
                    Set.of(TransactionType.TOP_UP, TransactionType.RENT_PAYMENT),
                    firstPage);

            assertThat(page.getTotalElements()).isEqualTo(2);
            assertThat(page.getContent())
                    .extracting(Transaction::getType)
                    .containsExactlyInAnyOrder(TransactionType.TOP_UP, TransactionType.RENT_PAYMENT);
        }

        @Test
        @DisplayName("does not leak another user's transactions of the same type")
        void doesNotLeakAcrossUsers() {
            persistTransaction(otherUser, TransactionType.TOP_UP);
            flushAndClear();

            assertThat(transactionRepository.findByUserIdAndTypeIn(
                    user.getId(), Set.of(TransactionType.TOP_UP), firstPage))
                    .isEmpty();
        }
    }

    @Nested
    @DisplayName("@EntityGraph prevents N+1")
    class NPlusOne {

        @Test
        @DisplayName("user history: rental.itemTitle loads without a query per row")
        void userHistoryQueryCountIsIndependentOfRowCount() {
            persistTransaction(user, TransactionType.TOP_UP, rental);
            persistTransaction(user, TransactionType.RENT_PAYMENT, rental);
            flushAndClear();

            long baseline = countQueriesForUserHistory();

            Item secondItem = persistItem(owner, "Circular Saw");
            Rental secondRental = persistRental(secondItem, user);
            persistTransaction(user, TransactionType.TOP_UP, secondRental);
            persistTransaction(user, TransactionType.RENT_PAYMENT, secondRental);
            flushAndClear();

            long grown = countQueriesForUserHistory();

            assertThat(grown)
                    .as("doubling the rows must not add queries — that would be N+1")
                    .isEqualTo(baseline);
        }

        private long countQueriesForUserHistory() {
            entityManager.clear();
            Statistics stats = statistics();
            stats.clear();

            Page<Transaction> page = transactionRepository
                    .findByUserIdOrderByCreatedAtDesc(user.getId(), PageRequest.of(0, 50));

            page.getContent().forEach(tx -> {
                if (tx.getRental() != null) {
                    assertThat(tx.getRental().getItemTitle()).isNotNull();
                }
            });

            return stats.getPrepareStatementCount();
        }
    }
}