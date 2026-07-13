package com.borrowly.model.user;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {
    private Validator validator;

    @BeforeEach
    void setUp() {
        // Sets up the Jakarta validator to test @NotBlank, @Email, @DecimalMin offline
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Test
    void builder_ProducesExpectedValues() {
        UUID id = UUID.randomUUID();
        User user = User.builder()
                .id(id)
                .firstName("Jane")
                .lastName("Doe")
                .email("jane.doe@example.com")
                .passwordHash("secureHash123")
                .phone("555-1234")
                .role(UserRole.ADMIN)
                .currentBalance(new BigDecimal("150.50"))
                .enabled(false)
                .build();

        assertEquals(id, user.getId());
        assertEquals("Jane", user.getFirstName());
        assertEquals("Doe", user.getLastName());
        assertEquals("jane.doe@example.com", user.getEmail());
        assertEquals("secureHash123", user.getPasswordHash());
        assertEquals("555-1234", user.getPhone());
        assertEquals(UserRole.ADMIN, user.getRole());
        assertEquals(new BigDecimal("150.50"), user.getCurrentBalance());
        assertFalse(user.getEnabled());
    }

    @Test
    void validation_TriggersOnBlankAndInvalidEmail() {
        User user = createValidUserBuilder()
                .firstName("")           // Invalid: @NotBlank
                .lastName(null)          // Invalid: @NotBlank
                .email("invalid-email")  // Invalid: @Email
                .passwordHash("   ")     // Invalid: @NotBlank
                .build();

        Set<ConstraintViolation<User>> violations = validator.validate(user);

        // We expect 4 violations for the 4 invalid fields above
        assertEquals(4, violations.size());

        boolean hasEmailViolation = violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("email"));
        assertTrue(hasEmailViolation, "Should trigger @Email violation");
    }

    @Test
    void validation_TriggersOnNegativeBalance() {
        User user = createValidUserBuilder()
                .currentBalance(new BigDecimal("-10.00")) // Invalid: @DecimalMin("0.00")
                .build();

        Set<ConstraintViolation<User>> violations = validator.validate(user);

        assertEquals(1, violations.size());
        assertEquals("currentBalance", violations.iterator().next().getPropertyPath().toString());
    }

    @Test
    void builder_PopulatesDefaultsAutomatically() {
        // Build user without specifying role, balance, or enabled status
        User user = User.builder()
                .firstName("John")
                .build();

        // Verify the @Builder.Default annotations populated the fields immediately
        assertEquals(UserRole.USER, user.getRole(), "Role should default to USER");
        assertEquals(BigDecimal.ZERO, user.getCurrentBalance(), "Balance should default to ZERO");
        assertTrue(user.getEnabled(), "Enabled should default to true");
    }

    @Test
    void equalsAndHashCode_ComparesByIdOnly() {
        UUID sharedId = UUID.randomUUID();

        User user1 = User.builder()
                .id(sharedId)
                .email("user1@example.com")
                .build();

        User user2 = User.builder()
                .id(sharedId)
                .email("completely.different@example.com")
                .build();

        User user3 = User.builder()
                .id(UUID.randomUUID())
                .email("user1@example.com")
                .build();

        // Same ID, different fields -> Should be equal
        assertEquals(user1, user2);
        assertEquals(user1.hashCode(), user2.hashCode());

        // Different ID, same fields -> Should NOT be equal
        assertNotEquals(user1, user3);
    }

    @Test
    void toString_NeverContainsPasswordHash() {
        String secretHash = "SUPER_SECRET_HASH_98765";
        User user = User.builder()
                .firstName("Test")
                .email("test@example.com")
                .passwordHash(secretHash)
                .build();

        String toStringResult = user.toString();

        assertTrue(toStringResult.contains("firstName=Test"));
        assertTrue(toStringResult.contains("email=test@example.com"));
        assertFalse(toStringResult.contains(secretHash), "toString MUST NOT leak the password hash!");
        assertFalse(toStringResult.contains("passwordHash"), "toString MUST NOT contain the passwordHash field name!");
    }

    /**
     * Helper method to create a valid user baseline so we can test single failing fields.
     */
    private User.UserBuilder createValidUserBuilder() {
        return User.builder()
                .firstName("Valid")
                .lastName("User")
                .email("valid@example.com")
                .passwordHash("hash")
                .role(UserRole.USER)
                .currentBalance(BigDecimal.ZERO)
                .enabled(true);
    }
}
