package com.borrowly.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;


class JwtUtilTest {

    private static final String SECRET =
            "test-secret-key-for-borrowly-unit-tests-must-be-at-least-32-bytes";
    private static final long ONE_HOUR_MS = 3_600_000L;

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = newJwtUtil(SECRET, ONE_HOUR_MS);
    }

    @Test
    @DisplayName("token round-trips the email as the subject")
    void tokenCarriesEmail() {
        String token = jwtUtil.generateToken("alice@example.com", "USER");

        assertThat(jwtUtil.getEmailFromToken(token)).isEqualTo("alice@example.com");
    }

    @Test
    @DisplayName("token round-trips the role claim")
    void tokenCarriesRole() {
        String token = jwtUtil.generateToken("root@example.com", "ADMIN");

        assertThat(jwtUtil.getRoleFromToken(token)).isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("a freshly generated token validates")
    void freshTokenIsValid() {
        String token = jwtUtil.generateToken("alice@example.com", "USER");

        assertThat(jwtUtil.validateJwtToken(token)).isTrue();
    }

    @Test
    @DisplayName("a tampered token is rejected, not thrown on")
    void tamperedTokenIsRejected() {
        String token = jwtUtil.generateToken("alice@example.com", "USER");

        String[] parts = token.split("\\.");
        char c = parts[1].charAt(0);
        parts[1] = (c == 'A' ? 'B' : 'A') + parts[1].substring(1);
        String tampered = parts[0] + "." + parts[1] + "." + parts[2];

        assertThat(jwtUtil.validateJwtToken(tampered)).isFalse();
    }

    @Test
    @DisplayName("a token signed with a foreign secret is rejected")
    void foreignSecretIsRejected() {
        JwtUtil attacker = newJwtUtil(
                "a-completely-different-secret-key-of-sufficient-length-here", ONE_HOUR_MS);
        String forged = attacker.generateToken("alice@example.com", "ADMIN");

        assertThat(jwtUtil.validateJwtToken(forged)).isFalse();
    }

    @Test
    @DisplayName("an expired token is rejected")
    void expiredTokenIsRejected() {
        JwtUtil shortLived = newJwtUtil(SECRET, -1000L);
        String expired = shortLived.generateToken("alice@example.com", "USER");

        assertThat(jwtUtil.validateJwtToken(expired)).isFalse();
    }

    @Test
    @DisplayName("garbage input is rejected without throwing")
    void garbageIsRejected() {
        assertThat(jwtUtil.validateJwtToken("not-a-jwt")).isFalse();
        assertThat(jwtUtil.validateJwtToken("")).isFalse();
    }

    private JwtUtil newJwtUtil(String secret, long expiration) {
        JwtUtil util = new JwtUtil();
        ReflectionTestUtils.setField(util, "secret", secret);
        ReflectionTestUtils.setField(util, "expiration", expiration);
        util.init();
        return util;
    }
}
