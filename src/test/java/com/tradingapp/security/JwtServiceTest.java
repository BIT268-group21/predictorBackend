package com.tradingapp.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.tradingapp.user.Role;
import com.tradingapp.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;

class JwtServiceTest {

    private static final String SECRET = "test-secret-key-that-is-at-least-32-bytes-long";
    private static final long ONE_HOUR = 3_600_000L;

    private final JwtService jwtService = new JwtService(SECRET, ONE_HOUR);

    private static User user(String email) {
        return new User("trader", email, "hash", Role.USER);
    }

    private static UserDetails userDetails(String email) {
        return org.springframework.security.core.userdetails.User.withUsername(email)
                .password("hash")
                .authorities("ROLE_USER")
                .build();
    }

    @Test
    void generatesTokenThatValidatesAndCarriesTheEmailAndRole() {
        String token = jwtService.generateToken(user("trader@example.com"));

        assertThat(token).isNotBlank();
        assertThat(jwtService.extractEmail(token)).isEqualTo("trader@example.com");
        assertThat(jwtService.extractRole(token)).isEqualTo("USER");
        assertThat(jwtService.isValid(token, userDetails("trader@example.com"))).isTrue();
    }

    @Test
    void rejectsTokenIssuedForAnotherUser() {
        String token = jwtService.generateToken(user("trader@example.com"));

        assertThat(jwtService.isValid(token, userDetails("someone-else@example.com"))).isFalse();
    }

    @Test
    void rejectsExpiredToken() {
        JwtService expiringService = new JwtService(SECRET, -1_000L);

        String token = expiringService.generateToken(user("trader@example.com"));

        assertThat(expiringService.isValid(token, userDetails("trader@example.com"))).isFalse();
    }

    @Test
    void rejectsTokenSignedWithAnotherSecret() {
        JwtService otherService = new JwtService("another-secret-key-that-is-32-bytes-plus", ONE_HOUR);
        String foreignToken = otherService.generateToken(user("trader@example.com"));

        assertThat(jwtService.isValid(foreignToken, userDetails("trader@example.com"))).isFalse();
    }

    @Test
    void rejectsGarbageToken() {
        assertThat(jwtService.isValid("not-a-jwt", userDetails("trader@example.com"))).isFalse();
    }

    @Test
    void exposesConfiguredExpiry() {
        assertThat(jwtService.getExpirationMs()).isEqualTo(ONE_HOUR);
    }
}
