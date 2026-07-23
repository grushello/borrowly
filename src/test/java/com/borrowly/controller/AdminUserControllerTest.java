package com.borrowly.controller;

import com.borrowly.config.SecurityConfig;
import com.borrowly.dto.response.UserResponse;
import com.borrowly.exception.CannotDisableSelfException;
import com.borrowly.exception.GlobalExceptionHandler;
import com.borrowly.exception.UserNotFoundException;
import com.borrowly.model.user.UserRole;
import com.borrowly.security.AuthEntryPointJwt;
import com.borrowly.security.AuthTokenFilter;
import com.borrowly.security.JwtUtil;
import com.borrowly.service.auth.UserDetailsServiceImpl;
import com.borrowly.service.user.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@WebMvcTest(AdminUserController.class)
@Import({SecurityConfig.class, AuthTokenFilter.class, AuthEntryPointJwt.class,
        GlobalExceptionHandler.class})
class AdminUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

    private UserResponse userResponse(UUID id, boolean enabled) {
        return new UserResponse(
                id, "Alice", "Smith", "alice@example.com", null,
                UserRole.USER, BigDecimal.ZERO, enabled,
                LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    @DisplayName("GET /api/admin/users without authentication returns 401")
    void listUsersUnauthenticatedReturns401() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isUnauthorized());

        verify(userService, never()).listUsers(any());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("GET /api/admin/users as USER returns 403")
    void listUsersAsUserReturns403() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isForbidden());

        verify(userService, never()).listUsers(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /api/admin/users as ADMIN returns 200 with a page")
    void listUsersAsAdminReturns200() throws Exception {
        Page<UserResponse> page = new PageImpl<>(
                List.of(userResponse(UUID.randomUUID(), true)));
        when(userService.listUsers(any())).thenReturn(page);

        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].email").value("alice@example.com"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("PATCH /api/admin/users/{id}/disable as ADMIN sets enabled=false")
    void disableUserTogglesEnabledOff() throws Exception {
        UUID id = UUID.randomUUID();
        when(userService.disableUser(id)).thenReturn(userResponse(id, false));

        mockMvc.perform(patch("/api/admin/users/{id}/disable", id)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));

        verify(userService).disableUser(id);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("PATCH /api/admin/users/{id}/enable as ADMIN sets enabled=true")
    void enableUserTogglesEnabledOn() throws Exception {
        UUID id = UUID.randomUUID();
        when(userService.enableUser(id)).thenReturn(userResponse(id, true));

        mockMvc.perform(patch("/api/admin/users/{id}/enable", id)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true));

        verify(userService).enableUser(id);
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("PATCH /api/admin/users/{id}/disable as USER returns 403")
    void disableUserAsUserReturns403() throws Exception {
        mockMvc.perform(patch("/api/admin/users/{id}/disable", UUID.randomUUID())
                        .with(csrf()))
                .andExpect(status().isForbidden());

        verify(userService, never()).disableUser(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("PATCH disable returns 409 when an admin targets their own account")
    void disableSelfReturns409() throws Exception {
        UUID id = UUID.randomUUID();
        when(userService.disableUser(id)).thenThrow(new CannotDisableSelfException());

        mockMvc.perform(patch("/api/admin/users/{id}/disable", id)
                        .with(csrf()))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("PATCH disable returns 404 when the target user does not exist")
    void disableMissingUserReturns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(userService.disableUser(id)).thenThrow(new UserNotFoundException(id));

        mockMvc.perform(patch("/api/admin/users/{id}/disable", id)
                        .with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("PATCH enable returns 404 when the target user does not exist")
    void enableMissingUserReturns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(userService.enableUser(id)).thenThrow(new UserNotFoundException(id));

        mockMvc.perform(patch("/api/admin/users/{id}/enable", id)
                        .with(csrf()))
                .andExpect(status().isNotFound());
    }
}