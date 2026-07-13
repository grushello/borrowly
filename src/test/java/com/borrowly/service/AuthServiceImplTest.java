package com.borrowly.service;

import com.borrowly.dto.request.LoginRequest;
import com.borrowly.dto.request.RegisterRequest;
import com.borrowly.dto.response.AuthResponse;
import com.borrowly.model.user.User;
import com.borrowly.model.user.UserRole;
import com.borrowly.repository.user.UserRepository;
import com.borrowly.security.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthServiceImpl authService;

    private RegisterRequest registerRequest() {
        return new RegisterRequest("Alice", "Smith", "alice@example.com", "s3cur3P@ss!", null);
    }

    @Test
    @DisplayName("register persists the user and returns a token")
    void registerPersistsAndReturnsToken() {
        stubSuccessfulRegistration();

        AuthResponse response = authService.register(registerRequest());

        verify(userRepository).save(any(User.class));
        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.email()).isEqualTo("alice@example.com");
        assertThat(response.role()).isEqualTo(UserRole.USER);
    }

    @Test
    @DisplayName("register stores the BCrypt hash, never the raw password")
    void registerStoresHashedPassword() {
        stubSuccessfulRegistration();

        authService.register(registerRequest());

        User saved = captureSavedUser();
        assertThat(saved.getPasswordHash()).isEqualTo("hashed");
        assertThat(saved.getPasswordHash()).isNotEqualTo("s3cur3P@ss!");
    }

    @Test
    @DisplayName("register always assigns USER and a zero balance")
    void registerAssignsUserRoleAndZeroBalance() {
        stubSuccessfulRegistration();

        authService.register(registerRequest());

        User saved = captureSavedUser();
        assertThat(saved.getRole()).isEqualTo(UserRole.USER);
        assertThat(saved.getCurrentBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(saved.getEnabled()).isTrue();
    }

    @Test
    @DisplayName("register stores an optional phone when supplied")
    void registerStoresPhoneWhenSupplied() {
        stubSuccessfulRegistration();
        RegisterRequest withPhone = new RegisterRequest(
                "Alice", "Smith", "alice@example.com", "s3cur3P@ss!", "+37060000000");

        authService.register(withPhone);

        assertThat(captureSavedUser().getPhone()).isEqualTo("+37060000000");
    }

    @Test
    @DisplayName("register rejects a duplicate email without saving")
    void registerRejectsDuplicateEmail() {
        when(userRepository.existsByEmailIgnoreCase("alice@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("alice@example.com");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("login delegates to the AuthenticationManager and returns a token")
    void loginReturnsToken() {
        User alice = User.register("Alice", "Smith", "alice@example.com", "hashed");
        when(authenticationManager.authenticate(any())).thenReturn(authentication());
        when(userRepository.findByEmailIgnoreCase("alice@example.com")).thenReturn(Optional.of(alice));
        when(jwtUtil.generateToken("alice@example.com", "USER")).thenReturn("jwt-token");

        AuthResponse response = authService.login(
                new LoginRequest("alice@example.com", "s3cur3P@ss!"));

        verify(authenticationManager).authenticate(any());
        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.email()).isEqualTo("alice@example.com");
        assertThat(response.role()).isEqualTo(UserRole.USER);
    }

    @Test
    @DisplayName("login embeds the persisted ADMIN role in the token")
    void loginEmbedsAdminRole() {
        User admin = User.register("Root", "Admin", "root@example.com", "hashed");
        ReflectionTestUtils.setField(admin, "role", UserRole.ADMIN);
        when(authenticationManager.authenticate(any())).thenReturn(authentication());
        when(userRepository.findByEmailIgnoreCase("root@example.com")).thenReturn(Optional.of(admin));
        when(jwtUtil.generateToken("root@example.com", "ADMIN")).thenReturn("admin-token");

        AuthResponse response = authService.login(
                new LoginRequest("root@example.com", "s3cur3P@ss!"));

        assertThat(response.role()).isEqualTo(UserRole.ADMIN);
        assertThat(response.token()).isEqualTo("admin-token");
    }

    @Test
    @DisplayName("login propagates bad credentials and issues no token")
    void loginRejectsBadCredentials() {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() ->
                authService.login(new LoginRequest("alice@example.com", "wrong")))
                .isInstanceOf(BadCredentialsException.class);

        verify(jwtUtil, never()).generateToken(any(), any());
    }

    @Test
    @DisplayName("login resolves a user who typed their email in mixed case")
    void loginAcceptsMixedCaseEmail() {
        User alice = User.register("Alice", "Smith", "alice@example.com", "hashed");
        when(authenticationManager.authenticate(any())).thenReturn(authentication());
        when(userRepository.findByEmailIgnoreCase("ALICE@Example.com"))
                .thenReturn(Optional.of(alice));
        when(jwtUtil.generateToken("alice@example.com", "USER")).thenReturn("jwt-token");

        AuthResponse response = authService.login(
                new LoginRequest("ALICE@Example.com", "s3cur3P@ss!"));

        assertThat(response.email()).isEqualTo("alice@example.com");
    }

    private void stubSuccessfulRegistration() {
        when(userRepository.existsByEmailIgnoreCase("alice@example.com")).thenReturn(false);
        when(passwordEncoder.encode("s3cur3P@ss!")).thenReturn("hashed");
        when(jwtUtil.generateToken("alice@example.com", "USER")).thenReturn("jwt-token");
    }

    private User captureSavedUser() {
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        return captor.getValue();
    }

    private Authentication authentication() {
        return new UsernamePasswordAuthenticationToken("alice@example.com", null, List.of());
    }
}
