package com.borrowly.security;

import com.borrowly.model.user.User;
import com.borrowly.repository.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecurityContextCurrentUserProviderTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SecurityContextCurrentUserProvider provider;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("returns the persisted user matching the authenticated email")
    void returnsAuthenticatedUser() {
        authenticateAs("alice@example.com");
        User alice = User.register("Alice", "Smith", "alice@example.com", "hashed");
        when(userRepository.findByEmailIgnoreCase("alice@example.com")).thenReturn(Optional.of(alice));

        assertThat(provider.getCurrentUser()).isSameAs(alice);
    }

    @Test
    @DisplayName("throws when the security context holds no authentication")
    void throwsWhenNoAuthentication() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> provider.getCurrentUser())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No authenticated user");
    }

    @Test
    @DisplayName("throws for an anonymous principal rather than returning a user")
    void throwsForAnonymousPrincipal() {
        SecurityContextHolder.getContext().setAuthentication(
                new AnonymousAuthenticationToken(
                        "key",
                        "anonymousUser",
                        List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));

        assertThatThrownBy(() -> provider.getCurrentUser())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No authenticated user");
    }

    @Test
    @DisplayName("throws when the authenticated principal has no persisted user")
    void throwsWhenPrincipalHasNoPersistedUser() {
        authenticateAs("ghost@example.com");
        when(userRepository.findByEmailIgnoreCase("ghost@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> provider.getCurrentUser())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ghost@example.com");
    }

    private void authenticateAs(String email) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        email, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }
}
