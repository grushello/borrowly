package com.borrowly.mapper;

import com.borrowly.dto.response.TransactionResponse;
import com.borrowly.model.item.Item;
import com.borrowly.model.item.ItemCondition;
import com.borrowly.model.rental.Rental;
import com.borrowly.model.rental.RentalStatus;
import com.borrowly.model.transaction.Transaction;
import com.borrowly.model.transaction.TransactionStatus;
import com.borrowly.model.transaction.TransactionType;
import com.borrowly.model.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionMapperTest {

    private final TransactionMapper mapper = Mappers.getMapper(TransactionMapper.class);

    private User user;
    private Rental rental;

    @BeforeEach
    void setUp() {
        user = User.register("Bo", "Borrower", "borrower@borrowly.test", "hash");

        Item item = Item.builder()
                .owner(User.register("Ollie", "Owner", "owner@borrowly.test", "hash"))
                .title("Bosch Drill")
                .pricePerDay(new BigDecimal("5.00"))
                .depositAmount(new BigDecimal("50.00"))
                .finePerDay(new BigDecimal("2.50"))
                .condition(ItemCondition.GOOD)
                .build();

        rental = Rental.builder()
                .item(item)
                .borrower(user)
                .startDate(LocalDate.of(2026, Month.JULY, 10))
                .endDate(LocalDate.of(2026, Month.JULY, 15))
                .itemTitle("Bosch Drill")
                .dailyPrice(new BigDecimal("5.00"))
                .depositAmount(new BigDecimal("50.00"))
                .finePerDay(new BigDecimal("2.50"))
                .totalPrice(new BigDecimal("25.00"))
                .status(RentalStatus.ACTIVE)
                .build();
    }

    private Transaction.TransactionBuilder rentPayment() {
        return Transaction.builder()
                .user(user)
                .rental(rental)
                .amount(new BigDecimal("25.00"))
                .type(TransactionType.RENT_PAYMENT)
                .status(TransactionStatus.COMPLETED)
                .description("Rental payment for Bosch Drill");
    }

    private Transaction.TransactionBuilder topUp() {
        return Transaction.builder()
                .user(user)
                .amount(new BigDecimal("100.00"))
                .type(TransactionType.TOP_UP)
                .status(TransactionStatus.COMPLETED)
                .description("Wallet top-up");
    }

    @Nested
    @DisplayName("toResponse")
    class ToResponse {

        @Test
        @DisplayName("copies every scalar field across")
        void mapsScalarFields() {
            Transaction transaction = rentPayment().build();

            TransactionResponse response = mapper.toResponse(transaction);

            assertThat(response.id()).isEqualTo(transaction.getId());
            assertThat(response.amount()).isEqualByComparingTo("25.00");
            assertThat(response.type()).isEqualTo(TransactionType.RENT_PAYMENT);
            assertThat(response.status()).isEqualTo(TransactionStatus.COMPLETED);
            assertThat(response.description()).isEqualTo("Rental payment for Bosch Drill");
        }

        @Test
        @DisplayName("flattens rental to its ID — no rental payload in the history line")
        void flattensRentalId() {
            Transaction transaction = rentPayment().build();

            TransactionResponse response = mapper.toResponse(transaction);

            assertThat(response.rentalId())
                    .as("rental.id must be lifted onto rentalId")
                    .isEqualTo(rental.getId());
        }

        @Test
        @DisplayName("a null rental produces a null rentalId, not an NPE")
        void nullRentalGivesNullRentalId() {
            Transaction transaction = topUp().build();
            assertThat(transaction.getRental())
                    .as("a top-up is a wallet-only movement")
                    .isNull();

            TransactionResponse response = mapper.toResponse(transaction);

            assertThat(response.rentalId()).isNull();
            assertThat(response.type()).isEqualTo(TransactionType.TOP_UP);
            assertThat(response.amount()).isEqualByComparingTo("100.00");
        }

        @Test
        @DisplayName("a null description survives as null")
        void nullDescription() {
            Transaction transaction = topUp().description(null).build();

            assertThat(mapper.toResponse(transaction).description()).isNull();
        }

        @Test
        @DisplayName("a null transaction maps to null")
        void nullEntity() {
            assertThat(mapper.toResponse(null)).isNull();
        }

        @Test
        @DisplayName("maps a FAILED withdrawal — status is carried, not inferred")
        void mapsFailedWithdrawal() {
            Transaction transaction = Transaction.builder()
                    .user(user)
                    .amount(new BigDecimal("40.00"))
                    .type(TransactionType.WITHDRAWAL)
                    .status(TransactionStatus.FAILED)
                    .description("Insufficient balance")
                    .build();

            TransactionResponse response = mapper.toResponse(transaction);

            assertThat(response.status()).isEqualTo(TransactionStatus.FAILED);
            assertThat(response.type()).isEqualTo(TransactionType.WITHDRAWAL);
            assertThat(response.rentalId()).isNull();
        }
    }
}