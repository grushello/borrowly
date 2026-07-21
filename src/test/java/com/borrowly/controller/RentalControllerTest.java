package com.borrowly.controller;

import com.borrowly.config.SecurityConfig;
import com.borrowly.dto.response.RentalResponse;
import com.borrowly.exception.GlobalExceptionHandler;
import com.borrowly.exception.RentalNotFoundException;
import com.borrowly.exception.RentalNotReturnableException;
import com.borrowly.model.rental.RentalStatus;
import com.borrowly.security.AuthEntryPointJwt;
import com.borrowly.security.AuthTokenFilter;
import com.borrowly.security.JwtUtil;
import com.borrowly.service.auth.UserDetailsServiceImpl;
import com.borrowly.service.rental.RentalService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RentalController.class)
@Import({SecurityConfig.class, AuthTokenFilter.class, AuthEntryPointJwt.class,
        GlobalExceptionHandler.class})
class RentalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RentalService rentalService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

    private RentalResponse rentalResponse(UUID id) {
        return new RentalResponse(
                id, null, null,
                LocalDate.now().minusDays(5), LocalDate.now(), LocalDate.now(),
                "Bosch Drill",
                new BigDecimal("5.00"), new BigDecimal("50.00"), new BigDecimal("2.00"),
                new BigDecimal("25.00"),
                RentalStatus.RETURNED,
                LocalDateTime.now());
    }

    @Test
    @DisplayName("listing rentals without authentication returns 401")
    void listUnauthenticatedReturns401() throws Exception {
        mockMvc.perform(get("/api/rentals/as-borrower"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    @DisplayName("as-borrower passes the status filter through")
    void listAsBorrowerWithStatusFilter() throws Exception {
        when(rentalService.listAsBorrower(any(), any(Pageable.class))).thenReturn(Page.empty());

        mockMvc.perform(get("/api/rentals/as-borrower")
                        .param("status", "ACTIVE")
                        .param("status", "OVERDUE"))
                .andExpect(status().isOk());

        ArgumentCaptor<List<RentalStatus>> statuses = ArgumentCaptor.captor();
        verify(rentalService).listAsBorrower(statuses.capture(), any(Pageable.class));
        assertThat(statuses.getValue())
                .containsExactly(RentalStatus.ACTIVE, RentalStatus.OVERDUE);
    }

    @Test
    @WithMockUser
    @DisplayName("as-borrower without a status filter passes null")
    void listAsBorrowerWithoutStatusFilter() throws Exception {
        when(rentalService.listAsBorrower(any(), any(Pageable.class))).thenReturn(Page.empty());

        mockMvc.perform(get("/api/rentals/as-borrower"))
                .andExpect(status().isOk());

        verify(rentalService).listAsBorrower(isNull(), any(Pageable.class));
    }

    @Test
    @WithMockUser
    @DisplayName("an unknown status value is rejected with 400")
    void listWithBogusStatusReturns400() throws Exception {
        mockMvc.perform(get("/api/rentals/as-borrower").param("status", "NOPE"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    @DisplayName("as-owner honours page and size but always orders newest first")
    void listAsOwnerIgnoresClientSort() throws Exception {
        when(rentalService.listAsOwner(any(), any(Pageable.class))).thenReturn(Page.empty());

        mockMvc.perform(get("/api/rentals/as-owner")
                        .param("page", "2")
                        .param("size", "5")
                        .param("sort", "borrower.passwordHash,desc"))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(rentalService).listAsOwner(isNull(), pageable.capture());

        assertThat(pageable.getValue().getPageNumber()).isEqualTo(2);
        assertThat(pageable.getValue().getPageSize()).isEqualTo(5);
        assertThat(pageable.getValue().getSort())
                .isEqualTo(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    @Test
    @WithMockUser
    @DisplayName("GET by id returns 403 when the caller is not a party to the rental")
    void getByIdReturns403ForStranger() throws Exception {
        UUID id = UUID.randomUUID();
        when(rentalService.getById(id)).thenThrow(new AccessDeniedException("nope"));

        mockMvc.perform(get("/api/rentals/{id}", id))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser
    @DisplayName("GET by id returns 404 when the rental does not exist")
    void getByIdReturns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(rentalService.getById(id)).thenThrow(new RentalNotFoundException(id));

        mockMvc.perform(get("/api/rentals/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    @DisplayName("confirming a return returns 200 and the updated rental")
    void returnHappyPath() throws Exception {
        UUID id = UUID.randomUUID();
        when(rentalService.returnRental(id)).thenReturn(rentalResponse(id));

        mockMvc.perform(patch("/api/rentals/{id}/return", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.status").value("RETURNED"))
                .andExpect(jsonPath("$.actualReturnDate").exists());

        verify(rentalService).returnRental(id);
    }

    @Test
    @DisplayName("confirming a return without authentication returns 401")
    void returnUnauthenticatedReturns401() throws Exception {
        mockMvc.perform(patch("/api/rentals/{id}/return", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    @DisplayName("confirming a return on an already returned rental gives 409")
    void returnAlreadyReturnedGives409() throws Exception {
        UUID id = UUID.randomUUID();
        when(rentalService.returnRental(id))
                .thenThrow(new RentalNotReturnableException(id, RentalStatus.RETURNED));

        mockMvc.perform(patch("/api/rentals/{id}/return", id))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser
    @DisplayName("confirming a return by a non-owner gives 403")
    void returnByNonOwnerGives403() throws Exception {
        UUID id = UUID.randomUUID();
        when(rentalService.returnRental(id)).thenThrow(new AccessDeniedException("nope"));

        mockMvc.perform(patch("/api/rentals/{id}/return", id))
                .andExpect(status().isForbidden());
    }
}
