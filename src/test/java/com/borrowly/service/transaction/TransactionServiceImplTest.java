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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceImplTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private TransactionMapper transactionMapper;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @InjectMocks
    private TransactionServiceImpl transactionService;

    @Captor
    private ArgumentCaptor<Transaction> txCaptor;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.register("Alice", "Smith", "alice@example.com", "hash");
        testUser.addBalance(new BigDecimal("100.00"));
    }

    @Nested
    @DisplayName("topUp")
    class TopUp {

        @Test
        @DisplayName("increases balance, creates TOP_UP transaction, and sends PAYMENT_RECEIVED notification")
        void topUpHappyPath() {
            when(currentUserProvider.getCurrentUser()).thenReturn(testUser);
            when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(transactionMapper.toResponse(any())).thenReturn(dummyResponse(TransactionType.TOP_UP));

            TransactionResponse response = transactionService.topUp(new TopUpRequest(new BigDecimal("50.00")));

            assertThat(testUser.getCurrentBalance()).isEqualByComparingTo("150.00");

            verify(userRepository).save(testUser);
            verify(transactionRepository).save(txCaptor.capture());
            Transaction saved = txCaptor.getValue();
            assertThat(saved.getType()).isEqualTo(TransactionType.TOP_UP);
            assertThat(saved.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
            assertThat(saved.getAmount()).isEqualByComparingTo("50.00");
            assertThat(saved.getUser()).isEqualTo(testUser);

            verify(notificationService).send(
                    eq(testUser), eq(NotificationType.PAYMENT_RECEIVED),
                    any(String.class), isNull(), any(Transaction.class));

            assertThat(response).isNotNull();
        }
    }

    @Nested
    @DisplayName("withdraw")
    class Withdraw {

        @Test
        @DisplayName("decreases balance, creates WITHDRAWAL transaction, and sends notification")
        void withdrawHappyPath() {
            when(currentUserProvider.getCurrentUser()).thenReturn(testUser);
            when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(transactionMapper.toResponse(any())).thenReturn(dummyResponse(TransactionType.WITHDRAWAL));

            transactionService.withdraw(new WithdrawRequest(new BigDecimal("30.00")));

            assertThat(testUser.getCurrentBalance()).isEqualByComparingTo("70.00");

            verify(userRepository).save(testUser);
            verify(transactionRepository).save(txCaptor.capture());
            assertThat(txCaptor.getValue().getType()).isEqualTo(TransactionType.WITHDRAWAL);

            verify(notificationService).send(
                    eq(testUser), eq(NotificationType.WITHDRAWAL_COMPLETED),
                    any(String.class), isNull(), any(Transaction.class));
        }

        @Test
        @DisplayName("throws InsufficientBalanceException when amount exceeds balance")
        void withdrawExceedsBalance() {
            when(currentUserProvider.getCurrentUser()).thenReturn(testUser);
            WithdrawRequest request = new WithdrawRequest(new BigDecimal("200.00"));

            assertThatThrownBy(() -> transactionService.withdraw(request))
                    .isInstanceOf(InsufficientBalanceException.class);

            verify(transactionRepository, never()).save(any());
            verify(userRepository, never()).save(any());
            verify(notificationService, never()).send(any(), any(), any(), any(), any());
            assertThat(testUser.getCurrentBalance()).isEqualByComparingTo("100.00");
        }
    }

    @Nested
    @DisplayName("internal rental methods")
    class InternalMethods {

        private Rental rental;

        @BeforeEach
        void setUp() {
            rental = mock(Rental.class);
        }

        @Test
        @DisplayName("holdDeposit creates DEPOSIT_HELD and decreases borrower balance")
        void holdDeposit() {
            when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            transactionService.holdDeposit(testUser, new BigDecimal("25.00"), rental);

            assertThat(testUser.getCurrentBalance()).isEqualByComparingTo("75.00");
            verify(transactionRepository).save(txCaptor.capture());
            assertThat(txCaptor.getValue().getType()).isEqualTo(TransactionType.DEPOSIT_HELD);
            assertThat(txCaptor.getValue().getRental()).isEqualTo(rental);
        }

        @Test
        @DisplayName("chargeRent creates RENT_PAYMENT and decreases borrower balance")
        void chargeRent() {
            when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            transactionService.chargeRent(testUser, new BigDecimal("40.00"), rental);

            assertThat(testUser.getCurrentBalance()).isEqualByComparingTo("60.00");
            verify(transactionRepository).save(txCaptor.capture());
            assertThat(txCaptor.getValue().getType()).isEqualTo(TransactionType.RENT_PAYMENT);
        }

        @Test
        @DisplayName("payoutRent creates RENT_PAYOUT and increases owner balance")
        void payoutRent() {
            when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            transactionService.payoutRent(testUser, new BigDecimal("40.00"), rental);

            assertThat(testUser.getCurrentBalance()).isEqualByComparingTo("140.00");
            verify(transactionRepository).save(txCaptor.capture());
            assertThat(txCaptor.getValue().getType()).isEqualTo(TransactionType.RENT_PAYOUT);
        }

        @Test
        @DisplayName("returnDeposit creates DEPOSIT_RETURN and increases borrower balance")
        void returnDeposit() {
            when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            transactionService.returnDeposit(testUser, new BigDecimal("25.00"), rental);

            assertThat(testUser.getCurrentBalance()).isEqualByComparingTo("125.00");
            verify(transactionRepository).save(txCaptor.capture());
            assertThat(txCaptor.getValue().getType()).isEqualTo(TransactionType.DEPOSIT_RETURN);
        }

        @Test
        @DisplayName("chargeFine creates FINE and decreases borrower balance (can go negative)")
        void chargeFine() {
            when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            transactionService.chargeFine(testUser, new BigDecimal("150.00"), rental);

            assertThat(testUser.getCurrentBalance()).isEqualByComparingTo("-50.00");
            verify(transactionRepository).save(txCaptor.capture());
            assertThat(txCaptor.getValue().getType()).isEqualTo(TransactionType.FINE);
        }

        @ParameterizedTest
        @ValueSource(strings = {"holdDeposit", "chargeRent", "payoutRent", "returnDeposit", "chargeFine"})
        @DisplayName("internal methods are annotated with MANDATORY propagation")
        void mandatoryPropagation(String methodName) throws Exception {
            Method method = TransactionServiceImpl.class.getDeclaredMethod(
                    methodName, User.class, BigDecimal.class, Rental.class);
            Transactional txAnnotation = method.getAnnotation(Transactional.class);

            assertThat(txAnnotation).isNotNull();
            assertThat(txAnnotation.propagation()).isEqualTo(Propagation.MANDATORY);
        }
    }

    private TransactionResponse dummyResponse(TransactionType type) {
        return new TransactionResponse(UUID.randomUUID(), BigDecimal.TEN, type,
                TransactionStatus.COMPLETED, null, null, null);
    }
}