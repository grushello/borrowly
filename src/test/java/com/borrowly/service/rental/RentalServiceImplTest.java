package com.borrowly.service.rental;

import com.borrowly.exception.RentalNotFoundException;
import com.borrowly.exception.RentalNotReturnableException;
import com.borrowly.mapper.RentalMapper;
import com.borrowly.model.item.Item;
import com.borrowly.model.item.ItemCondition;
import com.borrowly.model.item.ItemStatus;
import com.borrowly.model.notification.NotificationType;
import com.borrowly.model.rental.Rental;
import com.borrowly.model.rental.RentalStatus;
import com.borrowly.model.user.User;
import com.borrowly.model.user.UserRole;
import com.borrowly.repository.rental.RentalRepository;
import com.borrowly.security.CurrentUserProvider;
import com.borrowly.service.notification.NotificationService;
import com.borrowly.service.transaction.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RentalServiceImplTest {

    @Mock
    private RentalRepository rentalRepository;

    @Mock
    private TransactionService transactionService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private RentalMapper rentalMapper;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @InjectMocks
    private RentalServiceImpl rentalService;

    private final Pageable pageable = PageRequest.of(0, 20);

    private User owner;
    private User borrower;
    private Item item;
    private Rental rental;

    @BeforeEach
    void setUp() {
        owner = User.register("Olivia", "Owner", "owner@test.com", "hash");
        borrower = User.register("Ben", "Borrower", "borrower@test.com", "hash");
        borrower.addBalance(new BigDecimal("100.00"));

        item = Item.builder()
                .title("Bosch Drill")
                .pricePerDay(new BigDecimal("5.00"))
                .depositAmount(new BigDecimal("50.00"))
                .finePerDay(new BigDecimal("2.00"))
                .condition(ItemCondition.GOOD)
                .status(ItemStatus.RENTED)
                .owner(owner)
                .build();

        rental = rentalEndingOn(LocalDate.now());
    }

    private Rental rentalEndingOn(LocalDate endDate) {
        return Rental.builder()
                .item(item)
                .borrower(borrower)
                .startDate(endDate.minusDays(5))
                .endDate(endDate)
                .itemTitle("Bosch Drill")
                .dailyPrice(new BigDecimal("5.00"))
                .depositAmount(new BigDecimal("50.00"))
                .finePerDay(new BigDecimal("2.00"))
                .totalPrice(new BigDecimal("25.00"))
                .status(RentalStatus.ACTIVE)
                .build();
    }

    @Test
    @DisplayName("listAsBorrower without a status filter lists every rental")
    void listAsBorrowerWithoutStatus() {
        when(currentUserProvider.getCurrentUser()).thenReturn(borrower);
        when(rentalRepository.findByBorrower_Id(borrower.getId(), pageable))
                .thenReturn(Page.empty());

        rentalService.listAsBorrower(null, pageable);

        verify(rentalRepository).findByBorrower_Id(borrower.getId(), pageable);
        verify(rentalRepository, never()).findByBorrower_IdAndStatusIn(any(), any(), any());
    }

    @Test
    @DisplayName("listAsBorrower with a status filter narrows by status")
    void listAsBorrowerWithStatus() {
        List<RentalStatus> statuses = List.of(RentalStatus.ACTIVE, RentalStatus.OVERDUE);

        when(currentUserProvider.getCurrentUser()).thenReturn(borrower);
        when(rentalRepository.findByBorrower_IdAndStatusIn(borrower.getId(), statuses, pageable))
                .thenReturn(Page.empty());

        rentalService.listAsBorrower(statuses, pageable);

        verify(rentalRepository).findByBorrower_IdAndStatusIn(borrower.getId(), statuses, pageable);
        verify(rentalRepository, never()).findByBorrower_Id(any(), any());
    }

    @Test
    @DisplayName("listAsOwner lists rentals of the current user's items")
    void listAsOwnerWithoutStatus() {
        when(currentUserProvider.getCurrentUser()).thenReturn(owner);
        when(rentalRepository.findByItem_Owner_Id(owner.getId(), pageable))
                .thenReturn(Page.empty());

        rentalService.listAsOwner(null, pageable);

        verify(rentalRepository).findByItem_Owner_Id(owner.getId(), pageable);
    }

    @Test
    @DisplayName("listAsOwner with a status filter narrows by status")
    void listAsOwnerWithStatus() {
        List<RentalStatus> statuses = List.of(RentalStatus.RETURNED);

        when(currentUserProvider.getCurrentUser()).thenReturn(owner);
        when(rentalRepository.findByItem_Owner_IdAndStatusIn(owner.getId(), statuses, pageable))
                .thenReturn(Page.empty());

        rentalService.listAsOwner(statuses, pageable);

        verify(rentalRepository).findByItem_Owner_IdAndStatusIn(owner.getId(), statuses, pageable);
    }

    @Test
    @DisplayName("getById is allowed for the borrower, the item owner and an admin")
    void getByIdAllowedParties() {
        User admin = User.register("Ada", "Admin", "admin@test.com", "hash");
        ReflectionTestUtils.setField(admin, "role", UserRole.ADMIN);

        when(rentalRepository.findById(rental.getId())).thenReturn(Optional.of(rental));

        for (User viewer : List.of(borrower, owner, admin)) {
            when(currentUserProvider.getCurrentUser()).thenReturn(viewer);
            rentalService.getById(rental.getId());
        }

        verify(rentalMapper, times(3)).toResponse(rental);
    }

    @Test
    @DisplayName("getById is denied for an unrelated user")
    void getByIdDeniedForStranger() {
        User stranger = User.register("Sam", "Stranger", "stranger@test.com", "hash");

        when(rentalRepository.findById(rental.getId())).thenReturn(Optional.of(rental));
        when(currentUserProvider.getCurrentUser()).thenReturn(stranger);

        assertThatThrownBy(() -> rentalService.getById(rental.getId()))
                .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(rentalMapper);
    }

    @Test
    @DisplayName("getById throws when the rental does not exist")
    void getByIdMissing() {
        UUID id = UUID.randomUUID();
        when(rentalRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> rentalService.getById(id))
                .isInstanceOf(RentalNotFoundException.class);
    }

    @Test
    @DisplayName("return on time moves the deposit back and pays the owner, with no fine")
    void returnOnTime() {
        when(rentalRepository.findById(rental.getId())).thenReturn(Optional.of(rental));
        when(currentUserProvider.getCurrentUser()).thenReturn(owner);

        rentalService.returnRental(rental.getId());

        verify(transactionService).returnDeposit(borrower, new BigDecimal("50.00"), rental);
        verify(transactionService).payoutRent(owner, new BigDecimal("25.00"), rental);
        verify(transactionService, never()).chargeFine(any(), any(), any());
        verify(transactionService, never()).payoutFine(any(), any(), any());

        assertThat(rental.getStatus()).isEqualTo(RentalStatus.RETURNED);
        assertThat(rental.getActualReturnDate()).isEqualTo(LocalDate.now());
        assertThat(item.getStatus()).isEqualTo(ItemStatus.ACTIVE);
    }

    @Test
    @DisplayName("return two days late charges the borrower and credits the owner the fine")
    void returnOverdueChargesAndCreditsFine() {
        rental = rentalEndingOn(LocalDate.now().minusDays(2));

        when(rentalRepository.findById(rental.getId())).thenReturn(Optional.of(rental));
        when(currentUserProvider.getCurrentUser()).thenReturn(owner);

        rentalService.returnRental(rental.getId());

        // finePerDay 2.00 x 2 days overdue
        ArgumentCaptor<BigDecimal> charged = ArgumentCaptor.forClass(BigDecimal.class);
        verify(transactionService).chargeFine(eq(borrower), charged.capture(), eq(rental));
        assertThat(charged.getValue()).isEqualByComparingTo("4.00");

        ArgumentCaptor<BigDecimal> credited = ArgumentCaptor.forClass(BigDecimal.class);
        verify(transactionService).payoutFine(eq(owner), credited.capture(), eq(rental));
        assertThat(credited.getValue()).isEqualByComparingTo("4.00");

        verify(transactionService).returnDeposit(borrower, new BigDecimal("50.00"), rental);
        verify(transactionService).payoutRent(owner, new BigDecimal("25.00"), rental);
    }

    @Test
    @DisplayName("a fine the balance cannot fully cover is topped up from the deposit")
    void returnLateFineSplitsAcrossBalanceThenDeposit() {
        ReflectionTestUtils.setField(borrower, "currentBalance", new BigDecimal("1.00"));
        rental = rentalEndingOn(LocalDate.now().minusDays(2)); // fine 2.00 x 2 = 4.00

        when(rentalRepository.findById(rental.getId())).thenReturn(Optional.of(rental));
        when(currentUserProvider.getCurrentUser()).thenReturn(owner);

        rentalService.returnRental(rental.getId());

        // 1.00 comes off the balance, the remaining 3.00 off the deposit, so 47.00 is refunded
        verify(transactionService).chargeFine(borrower, new BigDecimal("1.00"), rental);
        verify(transactionService).payoutFine(owner, new BigDecimal("4.00"), rental);
        verify(transactionService).returnDeposit(borrower, new BigDecimal("47.00"), rental);
        verify(transactionService).payoutRent(owner, new BigDecimal("25.00"), rental);
    }

    @Test
    @DisplayName("a fine beyond balance and deposit is capped, and the owner absorbs the rest")
    void returnLateFineBeyondBalanceAndDepositIsWrittenOff() {
        ReflectionTestUtils.setField(borrower, "currentBalance", BigDecimal.ZERO);
        rental = rentalEndingOn(LocalDate.now().minusDays(26)); // fine 2.00 x 26 = 52.00 > 50.00 deposit

        when(rentalRepository.findById(rental.getId())).thenReturn(Optional.of(rental));
        when(currentUserProvider.getCurrentUser()).thenReturn(owner);

        rentalService.returnRental(rental.getId());

        // nothing on the balance, the whole 50.00 deposit goes to the owner, the extra 2.00 is written off
        verify(transactionService, never()).chargeFine(any(), any(), any());
        verify(transactionService).payoutFine(owner, new BigDecimal("50.00"), rental);
        verify(transactionService, never()).returnDeposit(any(), any(), any());
        verify(transactionService).payoutRent(owner, new BigDecimal("25.00"), rental);
    }

    @Test
    @DisplayName("an overdue rental can still be returned")
    void returnOverdueStatusRental() {
        rental = rentalEndingOn(LocalDate.now().minusDays(1));
        rental.markOverdue();

        when(rentalRepository.findById(rental.getId())).thenReturn(Optional.of(rental));
        when(currentUserProvider.getCurrentUser()).thenReturn(owner);

        rentalService.returnRental(rental.getId());

        assertThat(rental.getStatus()).isEqualTo(RentalStatus.RETURNED);
    }

    @Test
    @DisplayName("a rental whose borrow window has not opened yet cannot be returned")
    void returnBeforeStartDate() {
        rental = Rental.builder()
                .item(item)
                .borrower(borrower)
                .startDate(LocalDate.now().plusDays(3))
                .endDate(LocalDate.now().plusDays(6))
                .itemTitle("Bosch Drill")
                .dailyPrice(new BigDecimal("5.00"))
                .depositAmount(new BigDecimal("50.00"))
                .finePerDay(new BigDecimal("2.00"))
                .totalPrice(new BigDecimal("25.00"))
                .status(RentalStatus.ACTIVE)
                .build();

        when(rentalRepository.findById(rental.getId())).thenReturn(Optional.of(rental));
        when(currentUserProvider.getCurrentUser()).thenReturn(owner);

        assertThatThrownBy(() -> rentalService.returnRental(rental.getId()))
                .isInstanceOf(RentalNotReturnableException.class)
                .hasMessageContaining("before it starts");

        assertThat(rental.getActualReturnDate()).isNull();
        assertThat(rental.getStatus()).isEqualTo(RentalStatus.ACTIVE);
        verifyNoInteractions(transactionService, notificationService);
    }

    @Test
    @DisplayName("a rental returned on its first day is fine")
    void returnOnStartDate() {
        rental = rentalEndingOn(LocalDate.now().plusDays(2));
        ReflectionTestUtils.setField(rental, "startDate", LocalDate.now());

        when(rentalRepository.findById(rental.getId())).thenReturn(Optional.of(rental));
        when(currentUserProvider.getCurrentUser()).thenReturn(owner);

        rentalService.returnRental(rental.getId());

        assertThat(rental.getStatus()).isEqualTo(RentalStatus.RETURNED);
        assertThat(rental.getActualReturnDate()).isEqualTo(LocalDate.now());
    }

    @Test
    @DisplayName("returning an already returned rental is rejected")
    void returnAlreadyReturned() {
        rental.returnItem(LocalDate.now());

        when(rentalRepository.findById(rental.getId())).thenReturn(Optional.of(rental));
        when(currentUserProvider.getCurrentUser()).thenReturn(owner);

        assertThatThrownBy(() -> rentalService.returnRental(rental.getId()))
                .isInstanceOf(RentalNotReturnableException.class);

        verifyNoInteractions(transactionService, notificationService);
    }

    @Test
    @DisplayName("only the item owner can confirm a return")
    void returnDeniedForNonOwner() {
        when(rentalRepository.findById(rental.getId())).thenReturn(Optional.of(rental));
        when(currentUserProvider.getCurrentUser()).thenReturn(borrower);

        assertThatThrownBy(() -> rentalService.returnRental(rental.getId()))
                .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(transactionService, notificationService);
    }

    @Test
    @DisplayName("return notifies the borrower and the owner once each")
    void returnNotifiesBothParties() {
        rental = rentalEndingOn(LocalDate.now().minusDays(2));

        when(rentalRepository.findById(rental.getId())).thenReturn(Optional.of(rental));
        when(currentUserProvider.getCurrentUser()).thenReturn(owner);

        rentalService.returnRental(rental.getId());

        ArgumentCaptor<User> recipients = ArgumentCaptor.forClass(User.class);
        ArgumentCaptor<String> messages = ArgumentCaptor.forClass(String.class);

        verify(notificationService, times(2)).send(
                recipients.capture(),
                eq(NotificationType.ITEM_RETURNED),
                messages.capture(),
                eq(rental),
                isNull());

        assertThat(recipients.getAllValues()).containsExactly(borrower, owner);
        assertThat(messages.getAllValues().get(0)).contains("Deposit returned: 50.00", "Fine charged: 4.00");
        assertThat(messages.getAllValues().get(1)).contains("Payout: 25.00", "Fine credited: 4.00");
    }

    @Test
    @DisplayName("a zero deposit and zero rent move no money at all")
    void returnWithNothingToMove() {
        rental = Rental.builder()
                .item(item)
                .borrower(borrower)
                .startDate(LocalDate.now().minusDays(1))
                .endDate(LocalDate.now())
                .itemTitle("Bosch Drill")
                .dailyPrice(BigDecimal.ZERO)
                .depositAmount(BigDecimal.ZERO)
                .finePerDay(BigDecimal.ZERO)
                .totalPrice(BigDecimal.ZERO)
                .status(RentalStatus.ACTIVE)
                .build();

        when(rentalRepository.findById(rental.getId())).thenReturn(Optional.of(rental));
        when(currentUserProvider.getCurrentUser()).thenReturn(owner);

        rentalService.returnRental(rental.getId());

        verifyNoInteractions(transactionService);
        verify(notificationService, times(2)).send(any(), any(), anyString(), any(), isNull());
    }

    @Test
    @DisplayName("return throws when the rental does not exist")
    void returnMissingRental() {
        UUID id = UUID.randomUUID();
        when(rentalRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> rentalService.returnRental(id))
                .isInstanceOf(RentalNotFoundException.class);
    }
}
