package com.borrowly.repository.notification;

import com.borrowly.model.item.Category;
import com.borrowly.model.item.Item;
import com.borrowly.model.item.ItemCondition;
import com.borrowly.model.item.ItemStatus;
import com.borrowly.model.notification.Notification;
import com.borrowly.model.notification.NotificationType;
import com.borrowly.model.rental.Rental;
import com.borrowly.model.rental.RentalStatus;
import com.borrowly.model.transaction.Transaction;
import com.borrowly.model.transaction.TransactionStatus;
import com.borrowly.model.transaction.TransactionType;
import com.borrowly.model.user.User;
import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;


@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class NotificationRepositoryTest {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private EntityManager entityManager;


    private User recipient;
    private User owner;
    private Category category;
    private Item item;
    private Rental rental;
    private Transaction transaction;


    @BeforeEach
    void setUp() {

        owner = User.register(
                "Owner",
                "User",
                "owner@test.com",
                "password"
        );

        recipient = User.register(
                "John",
                "Doe",
                "john@test.com",
                "password"
        );

        entityManager.persist(owner);
        entityManager.persist(recipient);


        category = Category.builder()
                .name("Tools")
                .description("Tools category")
                .build();

        entityManager.persist(category);


        item = Item.builder()
                .title("Drill")
                .description("Electric drill")
                .pricePerDay(BigDecimal.valueOf(10))
                .depositAmount(BigDecimal.valueOf(50))
                .finePerDay(BigDecimal.valueOf(5))
                .condition(ItemCondition.GOOD)
                .status(ItemStatus.ACTIVE)
                .category(category)
                .owner(owner)
                .build();

        entityManager.persist(item);


        rental = Rental.builder()
                .item(item)
                .borrower(recipient)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(3))
                .itemTitle(item.getTitle())
                .dailyPrice(BigDecimal.valueOf(10))
                .depositAmount(BigDecimal.valueOf(50))
                .finePerDay(BigDecimal.valueOf(5))
                .totalPrice(BigDecimal.valueOf(30))
                .status(RentalStatus.ACTIVE)
                .build();

        entityManager.persist(rental);


        transaction = Transaction.builder()
                .amount(BigDecimal.valueOf(30))
                .type(TransactionType.RENT_PAYMENT)
                .status(TransactionStatus.COMPLETED)
                .description("Rental payment")
                .user(recipient)
                .rental(rental)
                .build();

        entityManager.persist(transaction);

        entityManager.flush();
    }


    @Test
    void shouldSaveNotificationWithRecipient() {

        Notification notification = Notification.builder()
                .message("Rental approved")
                .type(NotificationType.RENTAL_APPROVED)
                .recipient(recipient)
                .build();


        Notification saved = notificationRepository.saveAndFlush(notification);


        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getRecipient().getId())
                .isEqualTo(recipient.getId());
    }


    @Test
    void shouldListNotificationsOrderedByCreatedAtDesc() throws InterruptedException {

        Notification first = notificationRepository.saveAndFlush(
                Notification.builder()
                        .message("First notification")
                        .type(NotificationType.GENERAL)
                        .recipient(recipient)
                        .build()
        );


        Thread.sleep(10);


        Notification second = notificationRepository.saveAndFlush(
                Notification.builder()
                        .message("Second notification")
                        .type(NotificationType.GENERAL)
                        .recipient(recipient)
                        .build()
        );


        Page<Notification> page =
                notificationRepository.findByRecipient_IdOrderByCreatedAtDesc(
                        recipient.getId(),
                        PageRequest.of(0, 10)
                );


        assertThat(page.getContent())
                .containsExactly(second, first);
    }


    @Test
    void shouldFilterNotificationsByType() {

        notificationRepository.saveAndFlush(
                Notification.builder()
                        .message("Approved")
                        .type(NotificationType.RENTAL_APPROVED)
                        .recipient(recipient)
                        .build()
        );


        notificationRepository.saveAndFlush(
                Notification.builder()
                        .message("Rejected")
                        .type(NotificationType.RENTAL_REJECTED)
                        .recipient(recipient)
                        .build()
        );


        Page<Notification> page =
                notificationRepository.findByRecipient_IdAndTypeOrderByCreatedAtDesc(
                        recipient.getId(),
                        NotificationType.RENTAL_APPROVED,
                        PageRequest.of(0, 10)
                );


        assertThat(page.getContent())
                .hasSize(1)
                .first()
                .extracting(Notification::getType)
                .isEqualTo(NotificationType.RENTAL_APPROVED);
    }


    @Test
    void shouldCountNotificationsByRecipient() {

        notificationRepository.saveAndFlush(
                Notification.builder()
                        .message("One")
                        .type(NotificationType.GENERAL)
                        .recipient(recipient)
                        .build()
        );

        notificationRepository.saveAndFlush(
                Notification.builder()
                        .message("Two")
                        .type(NotificationType.GENERAL)
                        .recipient(recipient)
                        .build()
        );


        long count =
                notificationRepository.countByRecipient_Id(
                        recipient.getId()
                );


        assertThat(count).isEqualTo(2);
    }


    @Test
    void shouldSaveNotificationWithoutRentalAndTransaction() {

        Notification notification =
                Notification.builder()
                        .message("General notification")
                        .type(NotificationType.GENERAL)
                        .recipient(recipient)
                        .build();


        Notification saved =
                notificationRepository.saveAndFlush(notification);


        entityManager.clear();


        Notification reloaded =
                notificationRepository.findById(saved.getId())
                        .orElseThrow();


        assertThat(reloaded.getRental()).isNull();
        assertThat(reloaded.getTransaction()).isNull();
    }


    @Test
    void shouldSaveNotificationWithRentalAndTransactionAndReloadRelations() {

        Notification notification =
                Notification.builder()
                        .message("Payment received")
                        .type(NotificationType.PAYMENT_RECEIVED)
                        .recipient(recipient)
                        .rental(rental)
                        .transaction(transaction)
                        .build();


        Notification saved =
                notificationRepository.saveAndFlush(notification);


        entityManager.clear();


        Notification reloaded =
                notificationRepository.findById(saved.getId())
                        .orElseThrow();


        assertThat(reloaded.getRental()).isNotNull();
        assertThat(reloaded.getRental().getId())
                .isEqualTo(rental.getId());


        assertThat(reloaded.getTransaction()).isNotNull();
        assertThat(reloaded.getTransaction().getId())
                .isEqualTo(transaction.getId());
    }
}