package com.borrowly.service.transaction;

import com.borrowly.dto.request.TopUpRequest;
import com.borrowly.dto.request.WithdrawRequest;
import com.borrowly.dto.response.TransactionResponse;
import com.borrowly.model.rental.Rental;
import com.borrowly.model.transaction.Transaction;
import com.borrowly.model.transaction.TransactionType;
import com.borrowly.model.user.User;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.util.List;

public interface TransactionService {

    TransactionResponse topUp(TopUpRequest request);

    TransactionResponse withdraw(WithdrawRequest request);

    Page<TransactionResponse> getHistory(List<TransactionType> types, int page, int size);

    Transaction holdDeposit(User borrower, BigDecimal amount, Rental rental);

    Transaction chargeRent(User borrower, BigDecimal amount, Rental rental);

    Transaction payoutRent(User owner, BigDecimal amount, Rental rental);

    Transaction returnDeposit(User borrower, BigDecimal amount, Rental rental);

    Transaction chargeFine(User borrower, BigDecimal amount, Rental rental);
}