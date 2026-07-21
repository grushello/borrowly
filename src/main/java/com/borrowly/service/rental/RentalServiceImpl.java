package com.borrowly.service.rental;

import com.borrowly.dto.response.RentalResponse;
import com.borrowly.exception.RentalNotFoundException;
import com.borrowly.exception.RentalNotReturnableException;
import com.borrowly.mapper.RentalMapper;
import com.borrowly.model.item.Item;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class RentalServiceImpl implements RentalService {

    private final RentalRepository rentalRepository;
    private final TransactionService transactionService;
    private final NotificationService notificationService;
    private final RentalMapper rentalMapper;
    private final CurrentUserProvider currentUserProvider;

    @Override
    public Page<RentalResponse> listAsBorrower(List<RentalStatus> statuses, Pageable pageable) {
        UUID borrowerId = currentUserProvider.getCurrentUser().getId();

        Page<Rental> rentals = isEmpty(statuses)
                ? rentalRepository.findByBorrowerId(borrowerId, pageable)
                : rentalRepository.findByBorrowerIdAndStatusIn(borrowerId, statuses, pageable);

        return rentals.map(rentalMapper::toResponse);
    }

    @Override
    public Page<RentalResponse> listAsOwner(List<RentalStatus> statuses, Pageable pageable) {
        UUID ownerId = currentUserProvider.getCurrentUser().getId();

        Page<Rental> rentals = isEmpty(statuses)
                ? rentalRepository.findByOwnerId(ownerId, pageable)
                : rentalRepository.findByOwnerIdAndStatusIn(ownerId, statuses, pageable);

        return rentals.map(rentalMapper::toResponse);
    }

    @Override
    public RentalResponse getById(UUID id) {
        Rental rental = rentalRepository.findById(id)
                .orElseThrow(() -> new RentalNotFoundException(id));

        User currentUser = currentUserProvider.getCurrentUser();

        if (!canView(rental, currentUser)) {
            log.warn("Unauthorized rental access attempt rentalId={} userId={}",
                    id, currentUser.getId());
            throw new AccessDeniedException("You are not allowed to view this rental.");
        }

        return rentalMapper.toResponse(rental);
    }

    @Override
    @Transactional
    public RentalResponse returnRental(UUID id) {
        Rental rental = rentalRepository.findById(id)
                .orElseThrow(() -> new RentalNotFoundException(id));

        Item item = rental.getItem();
        User owner = item.getOwner();
        User currentUser = currentUserProvider.getCurrentUser();

        if (!Objects.equals(currentUser.getId(), owner.getId())) {
            log.warn("Unauthorized return attempt rentalId={} userId={}", id, currentUser.getId());
            throw new AccessDeniedException("Only the item owner can confirm a return.");
        }

        RentalStatus status = rental.getStatus();
        if (status != RentalStatus.ACTIVE && status != RentalStatus.OVERDUE) {
            throw new RentalNotReturnableException(id, status);
        }

        LocalDate today = LocalDate.now(ZoneId.of("Europe/Vilnius"));

        // A rental is ACTIVE from the moment it is approved, which can be days before the
        // borrow window opens. Returning one early would set an actualReturnDate before the
        // start date, which Rental and the rentals table both reject.
        if (today.isBefore(rental.getStartDate())) {
            throw new RentalNotReturnableException(id, rental.getStartDate());
        }

        long overdueDays = today.isAfter(rental.getEndDate())
                ? ChronoUnit.DAYS.between(rental.getEndDate(), today)
                : 0;
        BigDecimal fine = rental.getFinePerDay().multiply(BigDecimal.valueOf(overdueDays));

        rental.returnItem(today);

        User borrower = rental.getBorrower();

        // The transactions table rejects amounts under 0.01, and an item may be listed with
        // no deposit or no daily price, so only move money that is actually there.
        if (fine.signum() > 0) {
            transactionService.chargeFine(borrower, fine, rental);
            transactionService.payoutFine(owner, fine, rental);
        }
        if (rental.getDepositAmount().signum() > 0) {
            transactionService.returnDeposit(borrower, rental.getDepositAmount(), rental);
        }
        if (rental.getTotalPrice().signum() > 0) {
            transactionService.payoutRent(owner, rental.getTotalPrice(), rental);
        }

        item.setStatus(ItemStatus.ACTIVE);

        notificationService.send(borrower, NotificationType.ITEM_RETURNED,
                borrowerMessage(rental, fine, overdueDays), rental, null);
        notificationService.send(owner, NotificationType.ITEM_RETURNED,
                ownerMessage(rental, fine), rental, null);

        log.info("Rental id={} returned by ownerId={} overdueDays={} fine={}",
                id, owner.getId(), overdueDays, fine);

        return rentalMapper.toResponse(rental);
    }

    private static boolean canView(Rental rental, User user) {
        return Objects.equals(user.getId(), rental.getBorrower().getId())
                || Objects.equals(user.getId(), rental.getItem().getOwner().getId())
                || user.getRole() == UserRole.ADMIN;
    }

    private static String borrowerMessage(Rental rental, BigDecimal fine, long overdueDays) {
        String message = String.format(
                "You returned %s on %s. Rent paid: %s. Deposit returned: %s.",
                rental.getItemTitle(),
                rental.getActualReturnDate(),
                rental.getTotalPrice(),
                rental.getDepositAmount());

        if (fine.signum() > 0) {
            message += String.format(
                    " Fine charged: %s (%d day(s) overdue).", fine, overdueDays);
        }
        return message;
    }

    private static String ownerMessage(Rental rental, BigDecimal fine) {
        String message = String.format(
                "%s was returned on %s. Payout: %s.",
                rental.getItemTitle(),
                rental.getActualReturnDate(),
                rental.getTotalPrice());

        if (fine.signum() > 0) {
            message += String.format(" Fine credited: %s.", fine);
        }
        return message;
    }

    private static boolean isEmpty(List<RentalStatus> statuses) {
        return statuses == null || statuses.isEmpty();
    }
}
