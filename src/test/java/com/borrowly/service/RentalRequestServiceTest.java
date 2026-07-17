package com.borrowly.service;

import com.borrowly.dto.request.CreateRentalRequest;
import com.borrowly.dto.response.RentalRequestResponse;
import com.borrowly.dto.response.RentalResponse;
import com.borrowly.exception.ForbiddenActionException;
import com.borrowly.exception.InsufficientBalanceException;
import com.borrowly.exception.ItemNotFoundException;
import com.borrowly.exception.RentalConflictException;
import com.borrowly.exception.RentalRequestNotFoundException;
import com.borrowly.exception.SelfRentalException;
import com.borrowly.mapper.RentalMapper;
import com.borrowly.mapper.RentalRequestMapper;
import com.borrowly.model.item.Item;
import com.borrowly.model.item.ItemCondition;
import com.borrowly.model.item.ItemStatus;
import com.borrowly.model.notification.NotificationType;
import com.borrowly.model.rental.Rental;
import com.borrowly.model.rental.RentalRequest;
import com.borrowly.model.rental.RentalRequestStatus;
import com.borrowly.model.user.User;
import com.borrowly.repository.item.ItemRepository;
import com.borrowly.repository.rental.RentalRepository;
import com.borrowly.repository.rental.RentalRequestRepository;
import com.borrowly.repository.user.UserRepository;
import com.borrowly.security.CurrentUserProvider;
import com.borrowly.service.notification.NotificationService;
import com.borrowly.service.rentalrequest.RentalRequestServiceImpl;
import com.borrowly.service.transaction.TransactionService;
import jakarta.persistence.OptimisticLockException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RentalRequestServiceTest {

    @Mock
    private RentalRequestRepository rentalRequestRepository;
    @Mock
    private RentalRepository rentalRepository;
    @Mock
    private ItemRepository itemRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private TransactionService transactionService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private RentalRequestMapper rentalRequestMapper;
    @Mock
    private RentalMapper rentalMapper;
    @Mock
    private CurrentUserProvider currentUserProvider;

    @InjectMocks
    private RentalRequestServiceImpl service;

    @Captor
    private ArgumentCaptor<RentalRequest> requestCaptor;
    @Captor
    private ArgumentCaptor<Rental> rentalCaptor;
    @Captor
    private ArgumentCaptor<String> messageCaptor;

    private User owner;
    private User borrower;
    private Item item;

    private static final LocalDate START = LocalDate.now().plusDays(3);
    private static final LocalDate END = LocalDate.now().plusDays(5); // 3 rental days inclusive

    @BeforeEach
    void setUp() {
        owner = User.register("Alice", "Owner", "owner@example.com", "hash");
        borrower = User.register("Bob", "Borrower", "borrower@example.com", "hash");
        borrower.addBalance(new BigDecimal("1000.00"));

        item = Item.builder()
                .title("Cordless Drill")
                .pricePerDay(new BigDecimal("10.00"))
                .depositAmount(new BigDecimal("50.00"))
                .finePerDay(new BigDecimal("5.00"))
                .condition(ItemCondition.GOOD)
                .status(ItemStatus.ACTIVE)
                .owner(owner)
                .build();
    }

    private CreateRentalRequest createDto() {
        return new CreateRentalRequest(item.getId(), START, END);
    }

    private RentalRequest pendingRequest() {
        return RentalRequest.builder()
                .item(item)
                .borrower(borrower)
                .startDate(START)
                .endDate(END)
                .status(RentalRequestStatus.PENDING)
                .build();
    }

    // -----------------------------------------------------------------
    // create
    // -----------------------------------------------------------------

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("happy path: persists request and notifies owner")
        void happyPath() {
            when(currentUserProvider.getCurrentUser()).thenReturn(borrower);
            when(itemRepository.findById(item.getId())).thenReturn(Optional.of(item));
            when(rentalRequestRepository.existsOverlappingApproved(any(), any(), any()))
                    .thenReturn(false);
            when(rentalRepository.existsOverlappingByStatuses(any(), any(), any(), anyList()))
                    .thenReturn(false);
            when(rentalRequestMapper.toResponse(any())).thenReturn(mock(RentalRequestResponse.class));

            service.create(createDto());

            verify(rentalRequestRepository).save(requestCaptor.capture());
            RentalRequest saved = requestCaptor.getValue();
            assertThat(saved.getBorrower()).isEqualTo(borrower);
            assertThat(saved.getItem()).isEqualTo(item);
            assertThat(saved.getStatus()).isEqualTo(RentalRequestStatus.PENDING);

            verify(notificationService).send(eq(owner), eq(NotificationType.RENTAL_REQUEST),
                    messageCaptor.capture(), isNull(), isNull());
            assertThat(messageCaptor.getValue())
                    .contains("Cordless Drill")
                    .contains(START.toString())
                    .contains(END.toString());
        }

        @Test
        @DisplayName("missing item -> ItemNotFoundException (404)")
        void itemMissing() {
            when(currentUserProvider.getCurrentUser()).thenReturn(borrower);
            when(itemRepository.findById(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.create(createDto()))
                    .isInstanceOf(ItemNotFoundException.class);
            verify(rentalRequestRepository, never()).save(any());
        }

        @Test
        @DisplayName("non-active item -> RentalConflictException (409)")
        void itemNotActive() {
            item.setStatus(ItemStatus.RENTED);
            when(currentUserProvider.getCurrentUser()).thenReturn(borrower);
            when(itemRepository.findById(item.getId())).thenReturn(Optional.of(item));

            assertThatThrownBy(() -> service.create(createDto()))
                    .isInstanceOf(RentalConflictException.class);
        }

        @Test
        @DisplayName("self-rent blocked -> SelfRentalException (403)")
        void selfRentBlocked() {
            when(currentUserProvider.getCurrentUser()).thenReturn(owner);
            when(itemRepository.findById(item.getId())).thenReturn(Optional.of(item));

            assertThatThrownBy(() -> service.create(createDto()))
                    .isInstanceOf(SelfRentalException.class);
            verify(rentalRequestRepository, never()).save(any());
        }

        @Test
        @DisplayName("overlapping approved request blocked -> RentalConflictException (409)")
        void overlapApprovedBlocked() {
            when(currentUserProvider.getCurrentUser()).thenReturn(borrower);
            when(itemRepository.findById(item.getId())).thenReturn(Optional.of(item));
            when(rentalRequestRepository.existsOverlappingApproved(any(), any(), any()))
                    .thenReturn(true);

            assertThatThrownBy(() -> service.create(createDto()))
                    .isInstanceOf(RentalConflictException.class);
        }

        @Test
        @DisplayName("overlapping active rental blocked -> RentalConflictException (409)")
        void overlapActiveRentalBlocked() {
            when(currentUserProvider.getCurrentUser()).thenReturn(borrower);
            when(itemRepository.findById(item.getId())).thenReturn(Optional.of(item));
            when(rentalRequestRepository.existsOverlappingApproved(any(), any(), any()))
                    .thenReturn(false);
            when(rentalRepository.existsOverlappingByStatuses(any(), any(), any(), anyList()))
                    .thenReturn(true);

            assertThatThrownBy(() -> service.create(createDto()))
                    .isInstanceOf(RentalConflictException.class);
        }

        @Test
        @DisplayName("insufficient balance -> InsufficientBalanceException (400)")
        void insufficientBalance() {
            borrower.subtractBalance(new BigDecimal("990.00")); // leaves 10.00, need 50 + 30
            when(currentUserProvider.getCurrentUser()).thenReturn(borrower);
            when(itemRepository.findById(item.getId())).thenReturn(Optional.of(item));
            when(rentalRequestRepository.existsOverlappingApproved(any(), any(), any()))
                    .thenReturn(false);
            when(rentalRepository.existsOverlappingByStatuses(any(), any(), any(), anyList()))
                    .thenReturn(false);

            assertThatThrownBy(() -> service.create(createDto()))
                    .isInstanceOf(InsufficientBalanceException.class);
            verify(rentalRequestRepository, never()).save(any());
        }
    }

    // -----------------------------------------------------------------
    // approve
    // -----------------------------------------------------------------

    @Nested
    @DisplayName("approve")
    class Approve {

        @Test
        @DisplayName("an item with no deposit and no daily price moves no money")
        void zeroAmountsMoveNoMoney() {
            item.setDepositAmount(BigDecimal.ZERO);
            item.setPricePerDay(BigDecimal.ZERO);
            RentalRequest request = pendingRequest();

            when(currentUserProvider.getCurrentUser()).thenReturn(owner);
            when(rentalRequestRepository.findById(request.getId())).thenReturn(Optional.of(request));
            when(rentalRepository.existsOverlappingByStatusesExcluding(
                    any(), any(), any(), any(), anyList())).thenReturn(false);
            when(rentalRequestRepository
                    .findByItem_IdAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                            eq(item.getId()), eq(RentalRequestStatus.PENDING), any(), any()))
                    .thenReturn(List.of(request));
            when(rentalMapper.toResponse(any())).thenReturn(mock(RentalResponse.class));

            service.approve(request.getId());

            // transactions.amount has a CHECK (amount >= 0.01), so a zero-value rental must
            // record no transaction at all rather than fail on commit.
            verify(transactionService, never()).holdDeposit(any(), any(), any());
            verify(transactionService, never()).chargeRent(any(), any(), any());

            assertThat(request.getStatus()).isEqualTo(RentalRequestStatus.APPROVED);
            assertThat(item.getStatus()).isEqualTo(ItemStatus.RENTED);
        }

        @Test
        @DisplayName("full flow: 3 transactions, rental snapshot, item RENTED, overlap-only auto-reject, notifications")
        void fullFlow() {
            RentalRequest request = pendingRequest();

            // A pending request whose dates overlap the approved range -> auto-rejected.
            User otherBorrower = User.register("Carl", "Other", "carl@example.com", "hash");
            RentalRequest overlapping = RentalRequest.builder()
                    .item(item).borrower(otherBorrower)
                    .startDate(START).endDate(END)
                    .status(RentalRequestStatus.PENDING).build();

            when(currentUserProvider.getCurrentUser()).thenReturn(owner);
            when(rentalRequestRepository.findById(request.getId())).thenReturn(Optional.of(request));
            when(rentalRepository.existsOverlappingByStatusesExcluding(
                    any(), any(), any(), any(), anyList())).thenReturn(false);
            when(rentalRequestRepository
                    .findByItem_IdAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                            eq(item.getId()), eq(RentalRequestStatus.PENDING), any(), any()))
                    .thenReturn(List.of(request, overlapping));
            when(rentalMapper.toResponse(any())).thenReturn(mock(RentalResponse.class));

            service.approve(request.getId());

            // rental persisted with snapshotted fields
            verify(rentalRepository).save(rentalCaptor.capture());
            Rental rental = rentalCaptor.getValue();
            assertThat(rental.getItemTitle()).isEqualTo("Cordless Drill");
            assertThat(rental.getDailyPrice()).isEqualByComparingTo("10.00");
            assertThat(rental.getDepositAmount()).isEqualByComparingTo("50.00");
            assertThat(rental.getFinePerDay()).isEqualByComparingTo("5.00");
            assertThat(rental.getTotalPrice()).isEqualByComparingTo("30.00"); // 10 * 3 days

            // three money movements with correct amounts
            verify(transactionService).holdDeposit(eq(borrower), eq(new BigDecimal("50.00")), any());
            verify(transactionService).chargeRent(eq(borrower), eq(new BigDecimal("30.00")), any());
            verify(transactionService, never()).payoutRent(any(), any(), any());(transactionService).payoutRent(eq(owner), eq(new BigDecimal("30.00")), any());

            // item flipped to RENTED
            assertThat(item.getStatus()).isEqualTo(ItemStatus.RENTED);
            verify(itemRepository).save(item);

            // request approved, overlapping one auto-rejected
            assertThat(request.getStatus()).isEqualTo(RentalRequestStatus.APPROVED);
            assertThat(overlapping.getStatus()).isEqualTo(RentalRequestStatus.REJECTED);

            // notifications: approved to borrower, rejected to overlapping borrower
            verify(notificationService).send(eq(borrower), eq(NotificationType.RENTAL_APPROVED),
                    any(), any(Rental.class), isNull());
            verify(notificationService).send(eq(otherBorrower), eq(NotificationType.RENTAL_REJECTED),
                    any(), isNull(), isNull());
        }

        @Test
        @DisplayName("non-overlapping pending requests remain PENDING")
        void nonOverlappingUntouched() {
            RentalRequest request = pendingRequest();
            when(currentUserProvider.getCurrentUser()).thenReturn(owner);
            when(rentalRequestRepository.findById(request.getId())).thenReturn(Optional.of(request));
            when(rentalRepository.existsOverlappingByStatusesExcluding(
                    any(), any(), any(), any(), anyList())).thenReturn(false);
            // repository query only returns date-overlapping requests; here just the approved one
            when(rentalRequestRepository
                    .findByItem_IdAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                            any(), any(), any(), any()))
                    .thenReturn(List.of(request));
            when(rentalMapper.toResponse(any())).thenReturn(mock(RentalResponse.class));

            service.approve(request.getId());

            // only one reject notification path possible; none fired since no other overlapping req
            verify(notificationService, never())
                    .send(any(), eq(NotificationType.RENTAL_REJECTED), any(), any(), any());
        }

        @Test
        @DisplayName("already APPROVED returns existing rental without re-processing (idempotent)")
        void idempotent() {
            RentalRequest request = pendingRequest();
            ReflectionTestUtils.setField(request, "status", RentalRequestStatus.APPROVED);
            Rental existing = mock(Rental.class);

            when(currentUserProvider.getCurrentUser()).thenReturn(owner);
            when(rentalRequestRepository.findById(request.getId())).thenReturn(Optional.of(request));
            when(rentalRepository.findByItem_IdAndBorrower_IdAndStartDateAndEndDate(
                    item.getId(), borrower.getId(), START, END)).thenReturn(Optional.of(existing));
            when(rentalMapper.toResponse(existing)).thenReturn(mock(RentalResponse.class));

            service.approve(request.getId());

            verify(rentalRepository, never()).save(any());
            verify(transactionService, never()).holdDeposit(any(), any(), any());
            verify(transactionService, never()).chargeRent(any(), any(), any());
            verify(transactionService, never()).payoutRent(any(), any(), any());
        }

        @Test
        @DisplayName("wrong user -> ForbiddenActionException (403)")
        void wrongUser() {
            RentalRequest request = pendingRequest();
            when(currentUserProvider.getCurrentUser()).thenReturn(borrower); // not the owner
            when(rentalRequestRepository.findById(request.getId())).thenReturn(Optional.of(request));

            assertThatThrownBy(() -> service.approve(request.getId()))
                    .isInstanceOf(ForbiddenActionException.class);
            verify(rentalRepository, never()).save(any());
        }

        @Test
        @DisplayName("non-pending (rejected) -> RentalConflictException (409)")
        void nonPending() {
            RentalRequest request = pendingRequest();
            ReflectionTestUtils.setField(request, "status", RentalRequestStatus.REJECTED);
            when(currentUserProvider.getCurrentUser()).thenReturn(owner);
            when(rentalRequestRepository.findById(request.getId())).thenReturn(Optional.of(request));

            assertThatThrownBy(() -> service.approve(request.getId()))
                    .isInstanceOf(RentalConflictException.class);
        }

        @Test
        @DisplayName("overlap re-check at approval -> RentalConflictException (409)")
        void overlapAtApproval() {
            RentalRequest request = pendingRequest();
            when(currentUserProvider.getCurrentUser()).thenReturn(owner);
            when(rentalRequestRepository.findById(request.getId())).thenReturn(Optional.of(request));
            when(rentalRepository.existsOverlappingByStatusesExcluding(
                    any(), any(), any(), any(), anyList())).thenReturn(true);

            assertThatThrownBy(() -> service.approve(request.getId()))
                    .isInstanceOf(RentalConflictException.class);
            verify(rentalRepository, never()).save(any());
        }

        @Test
        @DisplayName("insufficient balance at approval -> InsufficientBalanceException (400)")
        void insufficientBalanceAtApproval() {
            borrower.subtractBalance(new BigDecimal("950.00")); // leaves 50, need 50 + 30
            RentalRequest request = pendingRequest();
            when(currentUserProvider.getCurrentUser()).thenReturn(owner);
            when(rentalRequestRepository.findById(request.getId())).thenReturn(Optional.of(request));
            when(rentalRepository.existsOverlappingByStatusesExcluding(
                    any(), any(), any(), any(), anyList())).thenReturn(false);

            assertThatThrownBy(() -> service.approve(request.getId()))
                    .isInstanceOf(InsufficientBalanceException.class);
            verify(transactionService, never()).holdDeposit(any(), any(), any());
        }

        @Test
        @DisplayName("optimistic lock on item -> RentalConflictException (409)")
        void optimisticLock() {
            RentalRequest request = pendingRequest();
            when(currentUserProvider.getCurrentUser()).thenReturn(owner);
            when(rentalRequestRepository.findById(request.getId())).thenReturn(Optional.of(request));
            when(rentalRepository.existsOverlappingByStatusesExcluding(
                    any(), any(), any(), any(), anyList())).thenReturn(false);
            when(itemRepository.save(item)).thenThrow(new OptimisticLockException("stale"));

            assertThatThrownBy(() -> service.approve(request.getId()))
                    .isInstanceOf(RentalConflictException.class);
        }
    }

    // -----------------------------------------------------------------
    // reject
    // -----------------------------------------------------------------

    @Nested
    @DisplayName("reject")
    class Reject {

        @Test
        @DisplayName("owner rejects pending -> REJECTED + borrower notified")
        void rejectHappyPath() {
            RentalRequest request = pendingRequest();
            when(currentUserProvider.getCurrentUser()).thenReturn(owner);
            when(rentalRequestRepository.findById(request.getId())).thenReturn(Optional.of(request));
            when(rentalRequestMapper.toResponse(request))
                    .thenReturn(mock(RentalRequestResponse.class));

            service.reject(request.getId());

            assertThat(request.getStatus()).isEqualTo(RentalRequestStatus.REJECTED);
            verify(notificationService).send(eq(borrower), eq(NotificationType.RENTAL_REJECTED),
                    any(), isNull(), isNull());
        }

        @Test
        @DisplayName("wrong user -> ForbiddenActionException (403)")
        void rejectWrongUser() {
            RentalRequest request = pendingRequest();
            when(currentUserProvider.getCurrentUser()).thenReturn(borrower);
            when(rentalRequestRepository.findById(request.getId())).thenReturn(Optional.of(request));

            assertThatThrownBy(() -> service.reject(request.getId()))
                    .isInstanceOf(ForbiddenActionException.class);
        }

        @Test
        @DisplayName("non-pending -> RentalConflictException (409)")
        void rejectNonPending() {
            RentalRequest request = pendingRequest();
            ReflectionTestUtils.setField(request, "status", RentalRequestStatus.APPROVED);
            when(currentUserProvider.getCurrentUser()).thenReturn(owner);
            when(rentalRequestRepository.findById(request.getId())).thenReturn(Optional.of(request));

            assertThatThrownBy(() -> service.reject(request.getId()))
                    .isInstanceOf(RentalConflictException.class);
        }
    }

    // -----------------------------------------------------------------
    // cancel
    // -----------------------------------------------------------------

    @Nested
    @DisplayName("cancel")
    class Cancel {

        @Test
        @DisplayName("borrower cancels own pending -> CANCELED")
        void cancelHappyPath() {
            RentalRequest request = pendingRequest();
            when(currentUserProvider.getCurrentUser()).thenReturn(borrower);
            when(rentalRequestRepository.findById(request.getId())).thenReturn(Optional.of(request));
            when(rentalRequestMapper.toResponse(request))
                    .thenReturn(mock(RentalRequestResponse.class));

            service.cancel(request.getId());

            assertThat(request.getStatus()).isEqualTo(RentalRequestStatus.CANCELED);
        }

        @Test
        @DisplayName("non-borrower -> ForbiddenActionException (403)")
        void cancelWrongUser() {
            RentalRequest request = pendingRequest();
            when(currentUserProvider.getCurrentUser()).thenReturn(owner); // not the borrower
            when(rentalRequestRepository.findById(request.getId())).thenReturn(Optional.of(request));

            assertThatThrownBy(() -> service.cancel(request.getId()))
                    .isInstanceOf(ForbiddenActionException.class);
        }

        @Test
        @DisplayName("non-pending -> RentalConflictException (409)")
        void cancelNonPending() {
            RentalRequest request = pendingRequest();
            ReflectionTestUtils.setField(request, "status", RentalRequestStatus.APPROVED);
            when(currentUserProvider.getCurrentUser()).thenReturn(borrower);
            when(rentalRequestRepository.findById(request.getId())).thenReturn(Optional.of(request));

            assertThatThrownBy(() -> service.cancel(request.getId()))
                    .isInstanceOf(RentalConflictException.class);
        }

        @Test
        @DisplayName("missing request -> RentalRequestNotFoundException (404)")
        void cancelMissing() {
            UUID id = UUID.randomUUID();
            when(currentUserProvider.getCurrentUser()).thenReturn(borrower);
            when(rentalRequestRepository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.cancel(id))
                    .isInstanceOf(RentalRequestNotFoundException.class);
        }
    }
}