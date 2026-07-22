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

        // Collect the fine from the borrower's balance first, then from the deposit; whatever
        // is left over is written off so the borrower never goes into debt. The slice of the
        // deposit that covers the fine goes to the owner instead of back to the borrower.
        BigDecimal deposit = rental.getDepositAmount();
        BigDecimal fineFromBalance = fine.min(borrower.getCurrentBalance());
        BigDecimal fineFromDeposit = fine.subtract(fineFromBalance).min(deposit);
        BigDecimal depositRefund = deposit.subtract(fineFromDeposit);
        BigDecimal finePaidToOwner = fineFromBalance.add(fineFromDeposit);

        // Amounts can be zero and the transactions table rejects anything under 0.01, so only
        // record the moves that actually shift money.
        if (fineFromBalance.signum() > 0) {
            transactionService.chargeFine(borrower, fineFromBalance, rental);
        }
        if (finePaidToOwner.signum() > 0) {
            transactionService.payoutFine(owner, finePaidToOwner, rental);
        }
        if (depositRefund.signum() > 0) {
            transactionService.returnDeposit(borrower, depositRefund, rental);
        }
        if (rental.getTotalPrice().signum() > 0) {
            transactionService.payoutRent(owner, rental.getTotalPrice(), rental);
        }

        item.setStatus(ItemStatus.ACTIVE);

        notificationService.send(borrower, NotificationType.ITEM_RETURNED,
                borrowerMessage(rental, overdueDays, fine, depositRefund, finePaidToOwner),
                rental, null);
        notificationService.send(owner, NotificationType.ITEM_RETURNED,
                ownerMessage(rental, overdueDays, fine, finePaidToOwner), rental, null);

        log.info("Rental id={} returned by ownerId={} overdueDays={} fine={}",
                id, owner.getId(), overdueDays, fine);

        return rentalMapper.toResponse(rental);
    }

    private static boolean canView(Rental rental, User user) {
        return Objects.equals(user.getId(), rental.getBorrower().getId())
                || Objects.equals(user.getId(), rental.getItem().getOwner().getId())
                || user.getRole() == UserRole.ADMIN;
    }

    private static String borrowerMessage(Rental rental, long overdueDays, BigDecimal fine,
                                          BigDecimal depositRefund, BigDecimal finePaidToOwner) {
        String title = rental.getItemTitle();
        LocalDate returnedOn = rental.getActualReturnDate();
        BigDecimal rent = rental.getTotalPrice();

        if (fine.signum() == 0) {
            return String.format(
                    "You returned '%s' on %s. Rent paid: %s, deposit returned: %s.",
                    title, returnedOn, rent, depositRefund);
        }
        if (depositRefund.compareTo(rental.getDepositAmount()) == 0) {
            return String.format(
                    "You returned '%s' on %s, %d day(s) late. Rent paid: %s, fine charged: %s. "
                            + "Deposit returned in full: %s.",
                    title, returnedOn, overdueDays, rent, finePaidToOwner, depositRefund);
        }
        if (depositRefund.signum() > 0) {
            return String.format(
                    "You returned '%s' on %s, %d day(s) late. Rent paid: %s, fine charged: %s. "
                            + "Part of your deposit covered the fine, so %s was returned.",
                    title, returnedOn, overdueDays, rent, finePaidToOwner, depositRefund);
        }
        return String.format(
                "You returned '%s' on %s, %d day(s) late. Rent paid: %s. Your fine of %s used up "
                        + "the entire deposit, so nothing was returned.",
                title, returnedOn, overdueDays, rent, finePaidToOwner);
    }

    private static String ownerMessage(Rental rental, long overdueDays,
                                       BigDecimal fine, BigDecimal finePaidToOwner) {
        String title = rental.getItemTitle();
        LocalDate returnedOn = rental.getActualReturnDate();
        BigDecimal rent = rental.getTotalPrice();

        if (fine.signum() == 0) {
            return String.format("'%s' was returned on %s. Payout: %s.", title, returnedOn, rent);
        }
        if (finePaidToOwner.compareTo(fine) == 0) {
            return String.format(
                    "'%s' was returned on %s, %d day(s) late. Payout: %s, plus a late fine of %s.",
                    title, returnedOn, overdueDays, rent, finePaidToOwner);
        }
        return String.format(
                "'%s' was returned on %s, %d day(s) late. Payout: %s, plus %s of the %s fine; "
                        + "the borrower could not cover the rest.",
                title, returnedOn, overdueDays, rent, finePaidToOwner, fine);
    }

    private static boolean isEmpty(List<RentalStatus> statuses) {
        return statuses == null || statuses.isEmpty();
    }
}
