package com.borrowly.service.auth;

import com.borrowly.model.user.User;
import com.borrowly.repository.user.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;

    private User alice() {
        return User.register("Alice", "Smith", "alice@example.com", "hashed-password");
    }

    @Test
    @DisplayName("loads the email as the principal and the hash as the password")
    void loadsEmailAndPasswordHash() {
        when(userRepository.findByEmailIgnoreCase("alice@example.com")).thenReturn(Optional.of(alice()));

        UserDetails details = userDetailsService.loadUserByUsername("alice@example.com");

        assertThat(details.getUsername()).isEqualTo("alice@example.com");
        assertThat(details.getPassword()).isEqualTo("hashed-password");
    }

    @Test
    @DisplayName("maps UserRole.USER to the ROLE_USER authority")
    void mapsUserRoleToPrefixedAuthority() {
        when(userRepository.findByEmailIgnoreCase("alice@example.com")).thenReturn(Optional.of(alice()));

        UserDetails details = userDetailsService.loadUserByUsername("alice@example.com");

        assertThat(details.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_USER");
    }

    @Test
    @DisplayName("maps UserRole.ADMIN to the ROLE_ADMIN authority")
    void mapsAdminRoleToPrefixedAuthority() {
        User admin = User.register("Root", "Admin", "root@example.com", "hashed");

        ReflectionTestUtils.setField(admin, "role", com.borrowly.model.user.UserRole.ADMIN);
        when(userRepository.findByEmailIgnoreCase("root@example.com")).thenReturn(Optional.of(admin));

        UserDetails details = userDetailsService.loadUserByUsername("root@example.com");

        assertThat(details.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_ADMIN");
    }

    @Test
    @DisplayName("a disabled account is surfaced as disabled")
    void disabledAccountIsDisabled() {
        User disabled = alice();
        disabled.setEnabled(false);
        when(userRepository.findByEmailIgnoreCase("alice@example.com")).thenReturn(Optional.of(disabled));

        UserDetails details = userDetailsService.loadUserByUsername("alice@example.com");

        assertThat(details.isEnabled()).isFalse();
    }

    @Test
    @DisplayName("throws UsernameNotFoundException for an unknown email")
    void throwsForUnknownEmail() {
        when(userRepository.findByEmailIgnoreCase("ghost@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("ghost@example.com"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("ghost@example.com");
    }

    @Test
    @DisplayName("resolves a user from a mixed-case email")
    void resolvesMixedCaseEmail() {
        when(userRepository.findByEmailIgnoreCase("Alice@Example.COM"))
                .thenReturn(Optional.of(alice()));

        UserDetails details = userDetailsService.loadUserByUsername("Alice@Example.COM");

        assertThat(details.getUsername()).isEqualTo("alice@example.com");
    }
}
