package com.borrowly.scheduler;

import com.borrowly.model.item.Item;
import com.borrowly.model.notification.NotificationType;
import com.borrowly.model.rental.Rental;
import com.borrowly.model.rental.RentalStatus;
import com.borrowly.model.user.User;
import com.borrowly.repository.rental.RentalRepository;
import com.borrowly.service.notification.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OverdueRentalSchedulerTest {

    private RentalRepository rentalRepository;
    private NotificationService notificationService;
    private TransactionTemplate transactionTemplate;

    private OverdueRentalScheduler scheduler;

    private Rental activeRental;
    private Rental overdueRental;

    private UUID activeRentalId;
    private UUID overdueRentalId;

    private User activeBorrower;
    private User overdueBorrower;

    private Item activeItem;
    private Item overdueItem;


    @BeforeEach
    void setUp() {

        rentalRepository = mock(RentalRepository.class);
        notificationService = mock(NotificationService.class);
        transactionTemplate = mock(TransactionTemplate.class);

        doAnswer(invocation -> {

            Consumer<TransactionStatus> action =
                    invocation.getArgument(0);

            action.accept(mock(TransactionStatus.class));

            return null;

        }).when(transactionTemplate)
                .executeWithoutResult(any());


        scheduler = new OverdueRentalScheduler(
                rentalRepository,
                notificationService,
                transactionTemplate
        );


        activeBorrower = mock(User.class);
        overdueBorrower = mock(User.class);

        activeItem = mock(Item.class);
        overdueItem = mock(Item.class);


        when(activeItem.getTitle())
                .thenReturn("Camera");

        when(overdueItem.getTitle())
                .thenReturn("Laptop");


        activeRentalId = UUID.randomUUID();
        overdueRentalId = UUID.randomUUID();


        activeRental = Rental.builder()
                .id(activeRentalId)
                .borrower(activeBorrower)
                .item(activeItem)
                .itemTitle("Camera")
                .startDate(LocalDate.now().minusDays(5))
                .endDate(LocalDate.now().minusDays(2))
                .dailyPrice(BigDecimal.TEN)
                .depositAmount(BigDecimal.TEN)
                .finePerDay(BigDecimal.ONE)
                .totalPrice(BigDecimal.TEN)
                .status(RentalStatus.ACTIVE)
                .build();


        overdueRental = Rental.builder()
                .id(overdueRentalId)
                .borrower(overdueBorrower)
                .item(overdueItem)
                .itemTitle("Laptop")
                .startDate(LocalDate.now().minusDays(8))
                .endDate(LocalDate.now().minusDays(3))
                .dailyPrice(BigDecimal.TEN)
                .depositAmount(BigDecimal.TEN)
                .finePerDay(BigDecimal.ONE)
                .totalPrice(BigDecimal.TEN)
                .status(RentalStatus.OVERDUE)
                .build();


        when(rentalRepository.findByEndDateBeforeAndStatusIn(
                any(),
                anyList()
        )).thenReturn(
                List.of(activeRental, overdueRental)
        );


        when(rentalRepository.findById(activeRentalId))
                .thenReturn(Optional.of(activeRental));


        when(rentalRepository.findById(overdueRentalId))
                .thenReturn(Optional.of(overdueRental));
    }


    @Test
    void shouldMarkActiveRentalOverdueAndNotifyBoth() {

        scheduler.markOverdueRentals();


        assertEquals(
                RentalStatus.OVERDUE,
                activeRental.getStatus()
        );


        verify(rentalRepository)
                .save(activeRental);


        verify(notificationService)
                .send(
                        eq(activeBorrower),
                        eq(NotificationType.RENTAL_OVERDUE),
                        anyString(),
                        eq(activeRental),
                        isNull()
                );


        verify(notificationService)
                .send(
                        eq(overdueBorrower),
                        eq(NotificationType.RENTAL_OVERDUE),
                        anyString(),
                        eq(overdueRental),
                        isNull()
                );
    }


    @Test
    void shouldNotMarkAlreadyOverdueRentalAgain() {

        scheduler.markOverdueRentals();


        assertEquals(
                RentalStatus.OVERDUE,
                overdueRental.getStatus()
        );


        verify(rentalRepository, never())
                .save(overdueRental);


        verify(notificationService)
                .send(
                        eq(overdueBorrower),
                        eq(NotificationType.RENTAL_OVERDUE),
                        anyString(),
                        eq(overdueRental),
                        isNull()
                );
    }


    @Test
    void shouldContinueProcessingWhenOneRentalFails() {


        doThrow(new RuntimeException("notification failed"))
                .when(notificationService)
                .send(
                        eq(activeBorrower),
                        eq(NotificationType.RENTAL_OVERDUE),
                        anyString(),
                        eq(activeRental),
                        isNull()
                );


        scheduler.markOverdueRentals();


        assertEquals(
                RentalStatus.OVERDUE,
                activeRental.getStatus()
        );


        verify(notificationService)
                .send(
                        eq(overdueBorrower),
                        eq(NotificationType.RENTAL_OVERDUE),
                        anyString(),
                        eq(overdueRental),
                        isNull()
                );
    }
}