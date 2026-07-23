package com.borrowly.security;

import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.InsufficientAuthenticationException;

import static org.assertj.core.api.Assertions.assertThat;

class AuthEntryPointJwtTest {

    @Test
    @DisplayName("commences with 401 Unauthorized, not 403 Forbidden")
    void commenceSends401() throws Exception {
        AuthEntryPointJwt entryPoint = new AuthEntryPointJwt();
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(
                new MockHttpServletRequest(),
                response,
                new InsufficientAuthenticationException("Full authentication is required"));

        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
        assertThat(response.getStatus()).isNotEqualTo(HttpServletResponse.SC_FORBIDDEN);
    }
}
