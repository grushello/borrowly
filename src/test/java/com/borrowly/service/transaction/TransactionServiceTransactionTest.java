package com.borrowly.service.transaction;

import com.borrowly.model.user.User;
import com.borrowly.support.AbstractPostgresTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.IllegalTransactionStateException;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class TransactionServiceTransactionTest extends AbstractPostgresTest {

    @Autowired
    private TransactionService transactionService;

    @Test
    @DisplayName("holdDeposit outside a transaction is rejected by MANDATORY propagation")
    void holdDepositOutsideTransactionIsRejected() {
        User borrower = User.register("Alice", "Smith", "alice@example.com", "hashed");
        BigDecimal amount = new BigDecimal("10.00");

        assertThatThrownBy(() -> transactionService.holdDeposit(borrower, amount, null))
                .isInstanceOf(IllegalTransactionStateException.class);
    }
}