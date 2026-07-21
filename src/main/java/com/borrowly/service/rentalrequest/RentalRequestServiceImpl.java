package com.borrowly.service.rentalrequest;

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
import com.borrowly.model.item.ItemStatus;
import com.borrowly.model.notification.NotificationType;
import com.borrowly.model.rental.Rental;
import com.borrowly.model.rental.RentalRequest;
import com.borrowly.model.rental.RentalRequestStatus;
import com.borrowly.model.rental.RentalStatus;
import com.borrowly.model.user.User;
import com.borrowly.repository.item.ItemRepository;
import com.borrowly.repository.rental.RentalRepository;
import com.borrowly.repository.rental.RentalRequestRepository;
import com.borrowly.repository.user.UserRepository;
import com.borrowly.security.CurrentUserProvider;
import com.borrowly.service.notification.NotificationService;
import com.borrowly.service.transaction.TransactionService;
import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RentalRequestServiceImpl implements RentalRequestService {

    private static final List<RentalStatus> BLOCKING_RENTAL_STATUSES =
            List.of(RentalStatus.ACTIVE, RentalStatus.OVERDUE);

    private final RentalRequestRepository rentalRequestRepository;
    private final RentalRepository rentalRepository;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final TransactionService transactionService;
    private final NotificationService notificationService;
    private final RentalRequestMapper rentalRequestMapper;
    private final RentalMapper rentalMapper;
    private final CurrentUserProvider currentUserProvider;

    @Override
    @Transactional
    public RentalRequestResponse create(CreateRentalRequest request) {
        User borrower = currentUserProvider.getCurrentUser();

        Item item = itemRepository.findById(request.itemId())
                .orElseThrow(() -> new ItemNotFoundException(request.itemId()));

        if (item.getStatus() != ItemStatus.ACTIVE) {
            throw new RentalConflictException(
                    "Item is not available for rent (status " + item.getStatus() + ")");
        }

        if (item.getOwner().getId().equals(borrower.getId())) {
            throw new SelfRentalException();
        }

        LocalDate startDate = request.startDate();
        LocalDate endDate = request.endDate();

        if (rentalRequestRepository.existsOverlappingApproved(item.getId(), startDate, endDate)) {
            throw new RentalConflictException(
                    "The item already has an approved request for the selected dates");
        }

        if (rentalRepository.existsOverlappingByStatuses(
                item.getId(), startDate, endDate, BLOCKING_RENTAL_STATUSES)) {
            throw new RentalConflictException(
                    "The item is already rented for the selected dates");
        }

        long days = rentalDays(startDate, endDate);
        BigDecimal totalCost = item.getDepositAmount()
                .add(item.getPricePerDay().multiply(BigDecimal.valueOf(days)));

        if (borrower.getCurrentBalance().compareTo(totalCost) < 0) {
            throw new InsufficientBalanceException(
                    "Insufficient balance: required " + totalCost
                            + ", available " + borrower.getCurrentBalance());
        }

        RentalRequest rentalRequest = RentalRequest.builder()
                .item(item)
                .borrower(borrower)
                .startDate(startDate)
                .endDate(endDate)
                .status(RentalRequestStatus.PENDING)
                .build();

        rentalRequestRepository.save(rentalRequest);

        notificationService.send(
                item.getOwner(),
                NotificationType.RENTAL_REQUEST,
                "New rental request for '" + item.getTitle() + "' from "
                        + startDate + " to " + endDate,
                null,
                null);

        log.info("Created rental request '{}' for item '{}'", rentalRequest.getId(), item.getId());
        return rentalRequestMapper.toResponse(rentalRequest);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RentalRequestResponse> getIncoming(RentalRequestStatus status, Pageable pageable) {
        User owner = currentUserProvider.getCurrentUser();

        Page<RentalRequest> requests = (status == null)
                ? rentalRequestRepository.findByItem_Owner_Id(owner.getId(), pageable)
                : rentalRequestRepository.findByItem_Owner_IdAndStatus(owner.getId(), status, pageable);

        return requests.map(rentalRequestMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RentalRequestResponse> getOutgoing(RentalRequestStatus status, Pageable pageable) {
        User borrower = currentUserProvider.getCurrentUser();

        Page<RentalRequest> requests = (status == null)
                ? rentalRequestRepository.findByBorrower_Id(borrower.getId(), pageable)
                : rentalRequestRepository.findByBorrower_IdAndStatus(borrower.getId(), status, pageable);

        return requests.map(rentalRequestMapper::toResponse);
    }

    @Override
    @Transactional
    public RentalResponse approve(UUID requestId) {
        User owner = currentUserProvider.getCurrentUser();
        RentalRequest rentalRequest = getRequestOrThrow(requestId);

        requireItemOwner(rentalRequest, owner);

        if (rentalRequest.getStatus() == RentalRequestStatus.APPROVED) {
            Rental existing = rentalRepository
                    .findByItem_IdAndBorrower_IdAndStartDateAndEndDate(
                            rentalRequest.getItem().getId(),
                            rentalRequest.getBorrower().getId(),
                            rentalRequest.getStartDate(),
                            rentalRequest.getEndDate())
                    .orElseThrow(() -> new RentalConflictException(
                            "Request is approved but its rental could not be found"));
            return rentalMapper.toResponse(existing);
        }

        if (rentalRequest.getStatus() != RentalRequestStatus.PENDING) {
            throw new RentalConflictException(
                    "Only pending requests can be approved, was " + rentalRequest.getStatus());
        }

        Item item = rentalRequest.getItem();
        User borrower = rentalRequest.getBorrower();
        LocalDate startDate = rentalRequest.getStartDate();
        LocalDate endDate = rentalRequest.getEndDate();

        if (rentalRepository.existsOverlappingByStatusesExcluding(
                item.getId(), startDate, endDate, rentalRequest.getId(), BLOCKING_RENTAL_STATUSES)) {
            throw new RentalConflictException(
                    "The item is already rented for the selected dates");
        }

        long days = rentalDays(startDate, endDate);
        BigDecimal dailyPrice = item.getPricePerDay();
        BigDecimal depositAmount = item.getDepositAmount();
        BigDecimal totalPrice = dailyPrice.multiply(BigDecimal.valueOf(days));

        if (borrower.getCurrentBalance().compareTo(depositAmount.add(totalPrice)) < 0) {
            throw new InsufficientBalanceException(
                    "Insufficient balance: required " + depositAmount.add(totalPrice)
                            + ", available " + borrower.getCurrentBalance());
        }

        Rental rental = Rental.builder()
                .item(item)
                .borrower(borrower)
                .startDate(startDate)
                .endDate(endDate)
                .itemTitle(item.getTitle())
                .dailyPrice(dailyPrice)
                .depositAmount(depositAmount)
                .finePerDay(item.getFinePerDay())
                .totalPrice(totalPrice)
                .status(RentalStatus.ACTIVE)
                .build();

        transactionService.holdDeposit(borrower, depositAmount, rental);
        transactionService.chargeRent(borrower, totalPrice, rental);

        rentalRepository.save(rental);

        try {
            item.setStatus(ItemStatus.RENTED);
            itemRepository.save(item);
            itemRepository.flush();
        } catch (OptimisticLockException | OptimisticLockingFailureException ex) {
            log.warn("Optimistic lock on item '{}' during approval of request '{}'",
                    item.getId(), rentalRequest.getId());
            throw new RentalConflictException(
                    "The item was modified concurrently, please retry");
        }

        rentalRequest.approve();

        List<RentalRequest> conflicts = rentalRequestRepository
                .findByItem_IdAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        item.getId(), RentalRequestStatus.PENDING, endDate, startDate);

        for (RentalRequest conflict : conflicts) {
            if (conflict.getId().equals(rentalRequest.getId())) {
                continue;
            }
            conflict.reject();
            notificationService.send(
                    conflict.getBorrower(),
                    NotificationType.RENTAL_REJECTED,
                    "Your request for '" + item.getTitle() + "' ("
                            + conflict.getStartDate() + " to " + conflict.getEndDate()
                            + ") was rejected because the item was rented for overlapping dates",
                    null,
                    null);
        }

        notificationService.send(
                borrower,
                NotificationType.RENTAL_APPROVED,
                "Your request for '" + item.getTitle() + "' ("
                        + startDate + " to " + endDate + ") was approved",
                rental,
                null);

        log.info("Approved rental request '{}', created rental '{}'",
                rentalRequest.getId(), rental.getId());
        return rentalMapper.toResponse(rental);
    }

    @Override
    @Transactional
    public RentalRequestResponse reject(UUID requestId) {
        User owner = currentUserProvider.getCurrentUser();
        RentalRequest rentalRequest = getRequestOrThrow(requestId);

        requireItemOwner(rentalRequest, owner);
        requirePending(rentalRequest);

        rentalRequest.reject();

        notificationService.send(
                rentalRequest.getBorrower(),
                NotificationType.RENTAL_REJECTED,
                "Your request for '" + rentalRequest.getItem().getTitle() + "' ("
                        + rentalRequest.getStartDate() + " to " + rentalRequest.getEndDate()
                        + ") was rejected",
                null,
                null);

        log.info("Rejected rental request '{}'", rentalRequest.getId());
        return rentalRequestMapper.toResponse(rentalRequest);
    }

    @Override
    @Transactional
    public RentalRequestResponse cancel(UUID requestId) {
        User borrower = currentUserProvider.getCurrentUser();
        RentalRequest rentalRequest = getRequestOrThrow(requestId);

        if (!rentalRequest.getBorrower().getId().equals(borrower.getId())) {
            throw new ForbiddenActionException("You can only cancel your own requests");
        }
        requirePending(rentalRequest);

        rentalRequest.cancel();

        log.info("Canceled rental request '{}'", rentalRequest.getId());
        return rentalRequestMapper.toResponse(rentalRequest);
    }

    private RentalRequest getRequestOrThrow(UUID requestId) {
        return rentalRequestRepository.findById(requestId)
                .orElseThrow(() -> new RentalRequestNotFoundException(requestId));
    }

    private void requireItemOwner(RentalRequest rentalRequest, User owner) {
        if (!rentalRequest.getItem().getOwner().getId().equals(owner.getId())) {
            throw new ForbiddenActionException("You do not own the item for this request");
        }
    }

    private void requirePending(RentalRequest rentalRequest) {
        if (!rentalRequest.isPending()) {
            throw new RentalConflictException(
                    "Only pending requests are allowed, was " + rentalRequest.getStatus());
        }
    }

    private long rentalDays(LocalDate startDate, LocalDate endDate) {
        return ChronoUnit.DAYS.between(startDate, endDate) + 1;
    }
}