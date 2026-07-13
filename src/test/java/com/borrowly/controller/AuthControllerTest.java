package com.borrowly.controller;

import com.borrowly.config.SecurityConfig;
import com.borrowly.dto.request.LoginRequest;
import com.borrowly.dto.request.RegisterRequest;
import com.borrowly.dto.response.AuthResponse;
import com.borrowly.exception.GlobalExceptionHandler;
import com.borrowly.model.user.UserRole;
import com.borrowly.security.AuthEntryPointJwt;
import com.borrowly.security.AuthTokenFilter;
import com.borrowly.security.JwtUtil;
import com.borrowly.service.AuthService;
import com.borrowly.service.UserDetailsServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, AuthTokenFilter.class, AuthEntryPointJwt.class,
        GlobalExceptionHandler.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

    private RegisterRequest validRegistration() {
        return new RegisterRequest("Alice", "Smith", "alice@example.com", "s3cur3P@ss!", null);
    }

    @Test
    @DisplayName("POST /api/auth/register returns 201 with a token")
    void registerReturns201() throws Exception {
        when(authService.register(any()))
                .thenReturn(new AuthResponse("jwt-token", "alice@example.com", UserRole.USER));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRegistration())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.email").value("alice@example.com"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    @DisplayName("register never returns a password hash")
    void registerDoesNotLeakPasswordHash() throws Exception {
        when(authService.register(any()))
                .thenReturn(new AuthResponse("jwt-token", "alice@example.com", UserRole.USER));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRegistration())))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    @DisplayName("register rejects a blank first name with 400 and never hits the service")
    void registerRejectsBlankFirstName() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "", "Smith", "alice@example.com", "s3cur3P@ss!", null);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(authService, never()).register(any());
    }

    @Test
    @DisplayName("register rejects a malformed email with 400")
    void registerRejectsMalformedEmail() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "Alice", "Smith", "not-an-email", "s3cur3P@ss!", null);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("register rejects a password with no special character with 400")
    void registerRejectsWeakPassword() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "Alice", "Smith", "alice@example.com", "plainpassword", null);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("register returns 409 when the email is already taken")
    void registerReturns409OnDuplicate() throws Exception {
        when(authService.register(any())).thenThrow(
                new IllegalArgumentException("Email already registered: alice@example.com"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRegistration())))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("POST /api/auth/login returns 200 with a token")
    void loginReturns200() throws Exception {
        when(authService.login(any()))
                .thenReturn(new AuthResponse("jwt-token", "alice@example.com", UserRole.USER));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("alice@example.com", "s3cur3P@ss!"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.email").value("alice@example.com"));
    }

    @Test
    @DisplayName("login returns 401 for bad credentials and leaks no token")
    void loginReturns401OnBadCredentials() throws Exception {
        when(authService.login(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("alice@example.com", "wrong-password"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.token").doesNotExist());
    }

    @Test
    @DisplayName("login rejects a blank password with 400")
    void loginRejectsBlankPassword() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("alice@example.com", ""))))
                .andExpect(status().isBadRequest());

        verify(authService, never()).login(any());
    }

    @Test
    @DisplayName("login rejects a malformed email with 400")
    void loginRejectsMalformedEmail() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("not-an-email", "s3cur3P@ss!"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("a protected route without a token returns 401, not 403")
    void protectedRouteWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/rentals"))
                .andExpect(status().isUnauthorized());
    }
}