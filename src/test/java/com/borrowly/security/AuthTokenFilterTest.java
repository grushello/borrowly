package com.borrowly.security;

import com.borrowly.service.UserDetailsServiceImpl;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthTokenFilterTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private UserDetailsServiceImpl userDetailsService;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private AuthTokenFilter authTokenFilter;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("a valid bearer token authenticates the request")
    void validTokenPopulatesSecurityContext() throws Exception {
        MockHttpServletRequest request = requestWithHeader("Bearer valid-token");

        UserDetails details = User.withUsername("alice@example.com")
                .password("hashed")
                .roles("USER")
                .build();

        when(jwtUtil.validateJwtToken("valid-token")).thenReturn(true);
        when(jwtUtil.getEmailFromToken("valid-token")).thenReturn("alice@example.com");
        when(userDetailsService.loadUserByUsername("alice@example.com")).thenReturn(details);

        authTokenFilter.doFilterInternal(request, new MockHttpServletResponse(), filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName())
                .isEqualTo("alice@example.com");
        verify(filterChain).doFilter(any(), any());
    }

    @Test
    @DisplayName("a valid token for a disabled account is rejected and sets no context")
    void disabledAccountIsRejected() throws Exception {
        MockHttpServletRequest request = requestWithHeader("Bearer valid-token");

        UserDetails disabled = User.withUsername("alice@example.com")
                .password("hashed")
                .roles("USER")
                .disabled(true)
                .build();

        when(jwtUtil.validateJwtToken("valid-token")).thenReturn(true);
        when(jwtUtil.getEmailFromToken("valid-token")).thenReturn("alice@example.com");
        when(userDetailsService.loadUserByUsername("alice@example.com")).thenReturn(disabled);

        authTokenFilter.doFilterInternal(request, new MockHttpServletResponse(), filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(any(), any());
    }

    @Test
    @DisplayName("an invalid token leaves the context empty and never loads a user")
    void invalidTokenLeavesContextEmpty() throws Exception {
        MockHttpServletRequest request = requestWithHeader("Bearer forged-token");
        when(jwtUtil.validateJwtToken("forged-token")).thenReturn(false);

        authTokenFilter.doFilterInternal(request, new MockHttpServletResponse(), filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(userDetailsService, never()).loadUserByUsername(anyString());
        verify(filterChain).doFilter(any(), any());
    }

    @Test
    @DisplayName("a request with no Authorization header passes through unauthenticated")
    void missingHeaderLeavesContextEmpty() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();

        authTokenFilter.doFilterInternal(request, new MockHttpServletResponse(), filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(any(), any());
    }

    @Test
    @DisplayName("a header without the Bearer prefix is ignored")
    void malformedHeaderIsIgnored() throws Exception {
        MockHttpServletRequest request = requestWithHeader("Basic YWxpY2U6cGFzcw==");

        authTokenFilter.doFilterInternal(request, new MockHttpServletResponse(), filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(any(), any());
    }

    @Test
    @DisplayName("an exception while loading the user is swallowed and the chain continues")
    void exceptionDuringLoadDoesNotBreakTheChain() throws Exception {
        MockHttpServletRequest request = requestWithHeader("Bearer valid-token");

        when(jwtUtil.validateJwtToken("valid-token")).thenReturn(true);
        when(jwtUtil.getEmailFromToken("valid-token")).thenReturn("ghost@example.com");
        when(userDetailsService.loadUserByUsername("ghost@example.com"))
                .thenThrow(new UsernameNotFoundException("User not found"));

        authTokenFilter.doFilterInternal(request, new MockHttpServletResponse(), filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(any(), any());
    }

    private MockHttpServletRequest requestWithHeader(String authorization) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", authorization);
        return request;
    }
}