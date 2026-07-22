package com.borrowly.controller;

import com.borrowly.config.SecurityConfig;
import com.borrowly.dto.response.TransactionResponse;
import com.borrowly.exception.GlobalExceptionHandler;
import com.borrowly.exception.InsufficientBalanceException;
import com.borrowly.model.transaction.TransactionStatus;
import com.borrowly.model.transaction.TransactionType;
import com.borrowly.security.AuthEntryPointJwt;
import com.borrowly.security.AuthTokenFilter;
import com.borrowly.security.JwtUtil;
import com.borrowly.service.transaction.TransactionService;
import com.borrowly.service.auth.UserDetailsServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.time.Month;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransactionController.class)
@Import({SecurityConfig.class, AuthTokenFilter.class, AuthEntryPointJwt.class,
        GlobalExceptionHandler.class})
class TransactionControllerTest {

    private static final LocalDateTime FIXED_TIME = LocalDateTime.of(2025, Month.JULY, 14, 12, 0);
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransactionService transactionService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

    private TransactionResponse sampleResponse(TransactionType type, BigDecimal amount) {
        return new TransactionResponse(
                UUID.randomUUID(), amount, type, TransactionStatus.COMPLETED,
                null, null, FIXED_TIME);
    }

    @Nested
    @DisplayName("POST /api/transactions/top-up")
    class TopUp {

        @Test
        @WithMockUser
        @DisplayName("returns 201 for a valid top-up")
        void topUpHappyPath() throws Exception {
            when(transactionService.topUp(any()))
                    .thenReturn(sampleResponse(TransactionType.TOP_UP, new BigDecimal("50.00")));

            mockMvc.perform(post("/api/transactions/top-up")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"amount\": 50.00}"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.type").value("TOP_UP"))
                    .andExpect(jsonPath("$.status").value("COMPLETED"));
        }

        @ParameterizedTest
        @ValueSource(strings = {"{\"amount\": 0}", "{\"amount\": -10}", "{}"})
        @WithMockUser
        @DisplayName("rejects invalid amounts with 400")
        void topUpRejectsInvalidAmount(String body) throws Exception {
            mockMvc.perform(post("/api/transactions/top-up")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());

            verify(transactionService, never()).topUp(any());
        }

        @Test
        @DisplayName("returns 401 without authentication")
        void topUpRequiresAuth() throws Exception {
            mockMvc.perform(post("/api/transactions/top-up")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"amount\": 50.00}"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("POST /api/transactions/withdraw")
    class Withdraw {

        @Test
        @WithMockUser
        @DisplayName("returns 201 for a valid withdrawal")
        void withdrawHappyPath() throws Exception {
            when(transactionService.withdraw(any()))
                    .thenReturn(sampleResponse(TransactionType.WITHDRAWAL, new BigDecimal("30.00")));

            mockMvc.perform(post("/api/transactions/withdraw")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"amount\": 30.00}"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.type").value("WITHDRAWAL"));
        }

        @Test
        @WithMockUser
        @DisplayName("returns 400 when balance is insufficient")
        void withdrawInsufficientBalance() throws Exception {
            when(transactionService.withdraw(any()))
                    .thenThrow(new InsufficientBalanceException("Insufficient balance"));

            mockMvc.perform(post("/api/transactions/withdraw")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"amount\": 9999.00}"))
                    .andExpect(status().isBadRequest());
        }

        @ParameterizedTest
        @ValueSource(strings = {"{\"amount\": 0}", "{\"amount\": -5}", "{}"})
        @WithMockUser
        @DisplayName("rejects invalid amounts with 400")
        void withdrawRejectsInvalidAmount(String body) throws Exception {
            mockMvc.perform(post("/api/transactions/withdraw")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());

            verify(transactionService, never()).withdraw(any());
        }

        @Test
        @DisplayName("returns 401 without authentication")
        void withdrawRequiresAuth() throws Exception {
            mockMvc.perform(post("/api/transactions/withdraw")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"amount\": 30.00}"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /api/transactions")
    class GetHistory {

        @Test
        @WithMockUser
        @DisplayName("returns 200 with paginated history")
        void historyHappyPath() throws Exception {
            TransactionResponse tx = sampleResponse(TransactionType.TOP_UP, new BigDecimal("10.00"));
            when(transactionService.getHistory(any(), eq(0), eq(20)))
                    .thenReturn(new PageImpl<>(List.of(tx)));

            mockMvc.perform(get("/api/transactions"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].type").value("TOP_UP"));
        }

        @Test
        @DisplayName("returns 401 without authentication")
        void historyRequiresAuth() throws Exception {
            mockMvc.perform(get("/api/transactions"))
                    .andExpect(status().isUnauthorized());
        }
    }
}