package com.borrowly.controller;

import com.borrowly.config.SecurityConfig;
import com.borrowly.dto.request.UpdateUserRequest;
import com.borrowly.dto.response.UserResponse;
import com.borrowly.dto.response.UserSummaryResponse;
import com.borrowly.exception.GlobalExceptionHandler;
import com.borrowly.exception.UserNotFoundException;
import com.borrowly.model.user.UserRole;
import com.borrowly.security.AuthEntryPointJwt;
import com.borrowly.security.AuthTokenFilter;
import com.borrowly.security.JwtUtil;
import com.borrowly.service.UserDetailsServiceImpl;
import com.borrowly.service.user.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, AuthTokenFilter.class, AuthEntryPointJwt.class,
        GlobalExceptionHandler.class})
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

    private static final LocalDateTime FIXED_TIME = LocalDateTime.of(2025, Month.JANUARY, 1, 0, 0);

    private UserResponse sampleResponse() {
        return new UserResponse(
                UUID.randomUUID(), "Alice", "Smith", "alice@example.com",
                "+37061234567", UserRole.USER, BigDecimal.ZERO,
                true, FIXED_TIME, FIXED_TIME);
    }

    @Test
    @DisplayName("GET /api/users/me without auth returns 401")
    void getMeUnauthenticatedReturns401() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());

        verify(userService, never()).getProfile();
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/users/me returns profile for authenticated user")
    void getMeReturnsProfile() throws Exception {
        UserResponse response = sampleResponse();
        when(userService.getProfile()).thenReturn(response);

        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("alice@example.com"))
                .andExpect(jsonPath("$.firstName").value("Alice"));
    }

    @Test
    @DisplayName("PATCH /api/users/me without auth returns 401")
    void patchMeUnauthenticatedReturns401() throws Exception {
        mockMvc.perform(patch("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());

        verify(userService, never()).updateProfile(any());
    }

    @Test
    @WithMockUser
    @DisplayName("PATCH /api/users/me with invalid phone returns 400")
    void patchMeInvalidPhoneReturns400() throws Exception {
        UpdateUserRequest request = new UpdateUserRequest(null, null, "not-a-phone");

        mockMvc.perform(patch("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.phone").value("Invalid phone format."));

        verify(userService, never()).updateProfile(any());
    }

    @Test
    @WithMockUser
    @DisplayName("PATCH /api/users/me with valid data returns updated profile")
    void patchMeValidDataReturnsUpdated() throws Exception {
        UserResponse updated = new UserResponse(
                UUID.randomUUID(), "Bob", "Smith", "alice@example.com",
                "+37061234567", UserRole.USER, BigDecimal.ZERO,
                true, FIXED_TIME, FIXED_TIME);
        when(userService.updateProfile(any())).thenReturn(updated);

        UpdateUserRequest request = new UpdateUserRequest("Bob", null, null);

        mockMvc.perform(patch("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Bob"))
                .andExpect(jsonPath("$.lastName").value("Smith"));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/users/{id} returns public summary")
    void getUserByIdReturnsSummary() throws Exception {
        UUID id = UUID.randomUUID();
        UserSummaryResponse summary = new UserSummaryResponse(id, "Alice", "Smith");
        when(userService.getUserSummary(id)).thenReturn(summary);

        mockMvc.perform(get("/api/users/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Alice"))
                .andExpect(jsonPath("$.email").doesNotExist());
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/users/{id} returns 404 when user not found")
    void getUserByIdNotFoundReturns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(userService.getUserSummary(id)).thenThrow(new UserNotFoundException(id));

        mockMvc.perform(get("/api/users/{id}", id))
                .andExpect(status().isNotFound());
    }
}