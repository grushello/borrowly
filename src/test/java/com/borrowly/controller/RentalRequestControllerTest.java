package com.borrowly.controller;

import com.borrowly.config.SecurityConfig;
import com.borrowly.dto.response.ItemSummaryResponse;
import com.borrowly.dto.response.RentalRequestResponse;
import com.borrowly.dto.response.RentalResponse;
import com.borrowly.dto.response.UserSummaryResponse;
import com.borrowly.exception.ForbiddenActionException;
import com.borrowly.exception.GlobalExceptionHandler;
import com.borrowly.exception.InsufficientBalanceException;
import com.borrowly.exception.RentalConflictException;
import com.borrowly.exception.RentalRequestNotFoundException;
import com.borrowly.exception.SelfRentalException;
import com.borrowly.model.item.ItemCondition;
import com.borrowly.model.item.ItemStatus;
import com.borrowly.model.rental.RentalRequestStatus;
import com.borrowly.model.rental.RentalStatus;
import com.borrowly.security.AuthEntryPointJwt;
import com.borrowly.security.AuthTokenFilter;
import com.borrowly.security.JwtUtil;
import com.borrowly.service.rentalrequest.RentalRequestService;
import com.borrowly.service.UserDetailsServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RentalRequestController.class)
@Import({SecurityConfig.class, AuthTokenFilter.class, AuthEntryPointJwt.class,
        GlobalExceptionHandler.class})
class RentalRequestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RentalRequestService rentalRequestService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

    private static final LocalDate START = LocalDate.now().plusDays(3);
    private static final LocalDate END = LocalDate.now().plusDays(5);

    private RentalRequestResponse sampleRequestResponse(RentalRequestStatus status) {
        return new RentalRequestResponse(
                UUID.randomUUID(),
                new ItemSummaryResponse(UUID.randomUUID(), "Drill", new BigDecimal("10.00"),
                        ItemCondition.GOOD, ItemStatus.ACTIVE, "Alice Owner", null),
                new UserSummaryResponse(UUID.randomUUID(), "Bob", "Borrower"),
                START, END, status, LocalDateTime.now());
    }

    private RentalResponse sampleRentalResponse() {
        return new RentalResponse(
                UUID.randomUUID(),
                new ItemSummaryResponse(UUID.randomUUID(), "Drill", new BigDecimal("10.00"),
                        ItemCondition.GOOD, ItemStatus.RENTED, "Alice Owner", null),
                new UserSummaryResponse(UUID.randomUUID(), "Bob", "Borrower"),
                START, END, null, "Drill",
                new BigDecimal("10.00"), new BigDecimal("50.00"), new BigDecimal("5.00"),
                new BigDecimal("30.00"), RentalStatus.ACTIVE, LocalDateTime.now());
    }

    private String body(LocalDate start, LocalDate end) {
        return "{\"itemId\":\"" + UUID.randomUUID() + "\",\"startDate\":\"" + start
                + "\",\"endDate\":\"" + end + "\"}";
    }

    // -----------------------------------------------------------------
    // POST /api/rental-requests
    // -----------------------------------------------------------------

    @Nested
    @DisplayName("POST /api/rental-requests")
    class Create {

        @Test
        @WithMockUser
        @DisplayName("returns 201 for a valid request")
        void happyPath() throws Exception {
            when(rentalRequestService.create(any()))
                    .thenReturn(sampleRequestResponse(RentalRequestStatus.PENDING));

            mockMvc.perform(post("/api/rental-requests")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body(START, END)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value("PENDING"));
        }

        @Test
        @WithMockUser
        @DisplayName("returns 400 when startDate is in the past")
        void pastStartDate() throws Exception {
            mockMvc.perform(post("/api/rental-requests")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body(LocalDate.now().minusDays(1), END)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser
        @DisplayName("returns 400 when endDate is not after startDate")
        void endBeforeStart() throws Exception {
            mockMvc.perform(post("/api/rental-requests")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body(START, START)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser
        @DisplayName("returns 400 when fields are missing")
        void missingFields() throws Exception {
            mockMvc.perform(post("/api/rental-requests")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser
        @DisplayName("returns 403 on self-rent")
        void selfRent() throws Exception {
            when(rentalRequestService.create(any())).thenThrow(new SelfRentalException());

            mockMvc.perform(post("/api/rental-requests")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body(START, END)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser
        @DisplayName("returns 409 on overlapping conflict")
        void conflict() throws Exception {
            when(rentalRequestService.create(any()))
                    .thenThrow(new RentalConflictException("overlap"));

            mockMvc.perform(post("/api/rental-requests")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body(START, END)))
                    .andExpect(status().isConflict());
        }

        @Test
        @WithMockUser
        @DisplayName("returns 400 on insufficient balance")
        void insufficientBalance() throws Exception {
            when(rentalRequestService.create(any()))
                    .thenThrow(new InsufficientBalanceException("low"));

            mockMvc.perform(post("/api/rental-requests")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body(START, END)))
                    .andExpect(status().isBadRequest());
        }
    }

    // -----------------------------------------------------------------
    // GET incoming / outgoing
    // -----------------------------------------------------------------

    @Nested
    @DisplayName("GET /api/rental-requests/incoming & /outgoing")
    class Listing {

        @Test
        @WithMockUser
        @DisplayName("incoming returns 200 with a page")
        void incoming() throws Exception {
            when(rentalRequestService.getIncoming(eq(RentalRequestStatus.PENDING), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(
                            List.of(sampleRequestResponse(RentalRequestStatus.PENDING))));

            mockMvc.perform(get("/api/rental-requests/incoming")
                            .param("status", "PENDING"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].status").value("PENDING"));
        }

        @Test
        @WithMockUser
        @DisplayName("outgoing returns 200 with a page")
        void outgoing() throws Exception {
            when(rentalRequestService.getOutgoing(any(), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(
                            List.of(sampleRequestResponse(RentalRequestStatus.PENDING))));

            mockMvc.perform(get("/api/rental-requests/outgoing"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray());
        }
    }

    // -----------------------------------------------------------------
    // PATCH approve / reject / cancel
    // -----------------------------------------------------------------

    @Nested
    @DisplayName("PATCH /api/rental-requests/{id}/approve")
    class Approve {

        @Test
        @WithMockUser
        @DisplayName("returns 200 + rental on success")
        void happyPath() throws Exception {
            UUID id = UUID.randomUUID();
            when(rentalRequestService.approve(id)).thenReturn(sampleRentalResponse());

            mockMvc.perform(patch("/api/rental-requests/{id}/approve", id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("ACTIVE"))
                    .andExpect(jsonPath("$.totalPrice").value(30.00));
        }

        @Test
        @WithMockUser
        @DisplayName("returns 403 when not the owner")
        void forbidden() throws Exception {
            UUID id = UUID.randomUUID();
            when(rentalRequestService.approve(id))
                    .thenThrow(new ForbiddenActionException("not owner"));

            mockMvc.perform(patch("/api/rental-requests/{id}/approve", id))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser
        @DisplayName("returns 409 when request is not pending")
        void conflict() throws Exception {
            UUID id = UUID.randomUUID();
            when(rentalRequestService.approve(id))
                    .thenThrow(new RentalConflictException("not pending"));

            mockMvc.perform(patch("/api/rental-requests/{id}/approve", id))
                    .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("PATCH /api/rental-requests/{id}/reject")
    class Reject {

        @Test
        @WithMockUser
        @DisplayName("returns 200 on success")
        void happyPath() throws Exception {
            UUID id = UUID.randomUUID();
            when(rentalRequestService.reject(id))
                    .thenReturn(sampleRequestResponse(RentalRequestStatus.REJECTED));

            mockMvc.perform(patch("/api/rental-requests/{id}/reject", id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("REJECTED"));
        }

        @Test
        @WithMockUser
        @DisplayName("returns 404 when request does not exist")
        void notFound() throws Exception {
            UUID id = UUID.randomUUID();
            when(rentalRequestService.reject(id))
                    .thenThrow(new RentalRequestNotFoundException(id));

            mockMvc.perform(patch("/api/rental-requests/{id}/reject", id))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PATCH /api/rental-requests/{id}/cancel")
    class Cancel {

        @Test
        @WithMockUser
        @DisplayName("returns 200 on success")
        void happyPath() throws Exception {
            UUID id = UUID.randomUUID();
            when(rentalRequestService.cancel(id))
                    .thenReturn(sampleRequestResponse(RentalRequestStatus.CANCELED));

            mockMvc.perform(patch("/api/rental-requests/{id}/cancel", id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CANCELED"));
        }

        @Test
        @WithMockUser
        @DisplayName("returns 409 when request is not pending")
        void conflict() throws Exception {
            UUID id = UUID.randomUUID();
            when(rentalRequestService.cancel(id))
                    .thenThrow(new RentalConflictException("not pending"));

            mockMvc.perform(patch("/api/rental-requests/{id}/cancel", id))
                    .andExpect(status().isConflict());
        }
    }
}