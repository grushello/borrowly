package com.borrowly.controller;

import com.borrowly.config.SecurityConfig;
import com.borrowly.dto.response.NotificationResponse;
import com.borrowly.exception.GlobalExceptionHandler;
import com.borrowly.model.notification.NotificationType;
import com.borrowly.security.AuthEntryPointJwt;
import com.borrowly.security.AuthTokenFilter;
import com.borrowly.security.JwtUtil;
import com.borrowly.service.UserDetailsServiceImpl;
import com.borrowly.service.notification.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.mockito.ArgumentCaptor;


@WebMvcTest(NotificationController.class)
@Import({SecurityConfig.class, AuthTokenFilter.class, AuthEntryPointJwt.class,
        GlobalExceptionHandler.class})
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationService notificationService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

    private NotificationResponse response() {
        return new NotificationResponse(
                UUID.randomUUID(),
                "Your request for 'Drill' was approved",
                NotificationType.RENTAL_APPROVED,
                null,
                null,
                LocalDateTime.now());
    }

    @Test
    @DisplayName("GET /api/notifications returns 200 with the caller's notifications")
    @WithMockUser
    void listReturns200() throws Exception {
        NotificationResponse notification = response();
        when(notificationService.listForCurrentUser(any()))
                .thenReturn(new PageImpl<>(List.of(notification), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].message")
                        .value("Your request for 'Drill' was approved"))
                .andExpect(jsonPath("$.content[0].type").value("RENTAL_APPROVED"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("GET /api/notifications without a token returns 401")
    void listWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isUnauthorized());

        verify(notificationService, org.mockito.Mockito.never()).listForCurrentUser(any());
    }

    @Test
    @DisplayName("page and size query params reach the service")
    @WithMockUser
    void paginationParamsArePassedThrough() throws Exception {
        when(notificationService.listForCurrentUser(any())).thenReturn(Page.empty());

        mockMvc.perform(get("/api/notifications").param("page", "2").param("size", "5"))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(notificationService).listForCurrentUser(captor.capture());
        assertThat(captor.getValue().getPageNumber()).isEqualTo(2);
        assertThat(captor.getValue().getPageSize()).isEqualTo(5);
    }

    @Test
    @DisplayName("a size above the configured maximum is capped at 50")
    @WithMockUser
    void oversizedPageRequestIsCapped() throws Exception {
        when(notificationService.listForCurrentUser(any())).thenReturn(Page.empty());

        mockMvc.perform(get("/api/notifications").param("size", "5000"))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(notificationService).listForCurrentUser(captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(50);
    }
}
