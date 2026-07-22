package com.borrowly.service.transaction;

import com.borrowly.dto.request.TopUpRequest;
import com.borrowly.dto.request.WithdrawRequest;
import com.borrowly.dto.response.TransactionResponse;
import com.borrowly.model.rental.Rental;
import com.borrowly.model.transaction.TransactionType;
import com.borrowly.model.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

public interface TransactionService {

    TransactionResponse topUp(TopUpRequest request);

    TransactionResponse withdraw(WithdrawRequest request);

    Page<TransactionResponse> getHistory(List<TransactionType> types, Pageable pageable);

    void holdDeposit(User borrower, BigDecimal amount, Rental rental);

    void chargeRent(User borrower, BigDecimal amount, Rental rental);

    void payoutRent(User owner, BigDecimal amount, Rental rental);

    void returnDeposit(User borrower, BigDecimal amount, Rental rental);

    void chargeFine(User borrower, BigDecimal amount, Rental rental);

    void payoutFine(User owner, BigDecimal amount, Rental rental);
}