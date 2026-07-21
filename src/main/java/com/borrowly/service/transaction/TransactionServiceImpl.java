package com.borrowly.service.transaction;

import com.borrowly.dto.request.TopUpRequest;
import com.borrowly.dto.request.WithdrawRequest;
import com.borrowly.dto.response.TransactionResponse;
import com.borrowly.exception.InsufficientBalanceException;
import com.borrowly.mapper.TransactionMapper;
import com.borrowly.model.notification.NotificationType;
import com.borrowly.model.rental.Rental;
import com.borrowly.model.transaction.Transaction;
import com.borrowly.model.transaction.TransactionStatus;
import com.borrowly.model.transaction.TransactionType;
import com.borrowly.model.user.User;
import com.borrowly.repository.transaction.TransactionRepository;
import com.borrowly.repository.user.UserRepository;
import com.borrowly.security.CurrentUserProvider;
import com.borrowly.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final TransactionMapper transactionMapper;
    private final CurrentUserProvider currentUserProvider;

    @Override
    @Transactional
    public TransactionResponse topUp(TopUpRequest request) {
        User user = currentUserProvider.getCurrentUser();
        user.addBalance(request.amount());

        Transaction tx = Transaction.builder()
                .amount(request.amount())
                .type(TransactionType.TOP_UP)
                .status(TransactionStatus.COMPLETED)
                .description("Balance top-up")
                .user(user)
                .build();

        userRepository.save(user);
        transactionRepository.save(tx);
        notificationService.send(user, NotificationType.PAYMENT_RECEIVED,
                "Top-up of " + request.amount() + " completed", null, tx);

        return transactionMapper.toResponse(tx);
    }

    @Override
    @Transactional
    public TransactionResponse withdraw(WithdrawRequest request) {
        User user = currentUserProvider.getCurrentUser();

        if (request.amount().compareTo(user.getCurrentBalance()) > 0) {
            throw new InsufficientBalanceException(
                    "Insufficient balance: requested " + request.amount()
                            + ", available " + user.getCurrentBalance());
        }

        user.subtractBalance(request.amount());

        Transaction tx = Transaction.builder()
                .amount(request.amount())
                .type(TransactionType.WITHDRAWAL)
                .status(TransactionStatus.COMPLETED)
                .description("Withdrawal")
                .user(user)
                .build();

        userRepository.save(user);
        transactionRepository.save(tx);
        notificationService.send(user, NotificationType.WITHDRAWAL_COMPLETED,
                "Withdrawal of " + request.amount() + " completed", null, tx);

        return transactionMapper.toResponse(tx);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TransactionResponse> getHistory(List<TransactionType> types, int page, int size) {
        User user = currentUserProvider.getCurrentUser();
        Pageable pageable = PageRequest.of(page, size);

        Page<Transaction> transactions = (types == null || types.isEmpty())
                ? transactionRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), pageable)
                : transactionRepository.findByUserIdAndTypeIn(user.getId(), types, pageable);

        return transactions.map(transactionMapper::toResponse);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void holdDeposit(User borrower, BigDecimal amount, Rental rental) {
        borrower.subtractBalance(amount);
        userRepository.save(borrower);
        saveRentalTransaction(borrower, amount, TransactionType.DEPOSIT_HELD, rental);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void chargeRent(User borrower, BigDecimal amount, Rental rental) {
        borrower.subtractBalance(amount);
        userRepository.save(borrower);
        saveRentalTransaction(borrower, amount, TransactionType.RENT_PAYMENT, rental);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void payoutRent(User owner, BigDecimal amount, Rental rental) {
        owner.addBalance(amount);
        userRepository.save(owner);
        saveRentalTransaction(owner, amount, TransactionType.RENT_PAYOUT, rental);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void returnDeposit(User borrower, BigDecimal amount, Rental rental) {
        borrower.addBalance(amount);
        userRepository.save(borrower);
        saveRentalTransaction(borrower, amount, TransactionType.DEPOSIT_RETURN, rental);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void chargeFine(User borrower, BigDecimal amount, Rental rental) {
        borrower.subtractBalance(amount);
        userRepository.save(borrower);
        saveRentalTransaction(borrower, amount, TransactionType.FINE, rental);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void payoutFine(User owner, BigDecimal amount, Rental rental) {
        owner.addBalance(amount);
        userRepository.save(owner);
        saveRentalTransaction(owner, amount, TransactionType.FINE_PAYOUT, rental);
    }

    private void saveRentalTransaction(User user, BigDecimal amount,
                                       TransactionType type, Rental rental) {
        Transaction tx = Transaction.builder()
                .amount(amount)
                .type(type)
                .status(TransactionStatus.COMPLETED)
                .user(user)
                .rental(rental)
                .build();
        transactionRepository.save(tx);
    }
}