package com.borrowly.repository.user;

import com.borrowly.model.user.User;
import com.borrowly.model.user.UserRole;
import com.borrowly.support.AbstractPostgresTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

// Liquibase owns the schema and the changesets are Postgres-specific, so there is no
// embedded database to swap in; AbstractPostgresTest starts a throwaway Postgres to run against.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserRepositoryTest extends AbstractPostgresTest {

    @Autowired
    private UserRepository userRepository;


    @Test
    void saveAndReloadUser() {
        User user = User.register(
                "Alice",
                "Smith",
                "alice@example.com",
                "hashedPassword"
        );

        User saved = userRepository.save(user);

        Optional<User> reloaded = userRepository.findById(saved.getId());

        assertTrue(reloaded.isPresent());
        assertEquals("alice@example.com", reloaded.get().getEmail());
        assertEquals("Alice", reloaded.get().getFirstName());
    }


    @Test
    void findByEmailIgnoreCase_returnsUser_whenEmailMatchesIgnoringCase() {
        User user = User.register(
                "Alice",
                "Smith",
                "alice@example.com",
                "hashedPassword"
        );

        userRepository.save(user);

        Optional<User> result =
                userRepository.findByEmailIgnoreCase("ALICE@EXAMPLE.COM");

        assertTrue(result.isPresent());
        assertEquals(user.getId(), result.get().getId());
    }


    @Test
    void findByEmailIgnoreCase_returnsEmpty_whenEmailDoesNotExist() {
        Optional<User> result =
                userRepository.findByEmailIgnoreCase("unknown@example.com");

        assertTrue(result.isEmpty());
    }


    @Test
    void existsByEmailIgnoreCase_returnsTrue_whenEmailExists() {
        User user = User.register(
                "Alice",
                "Smith",
                "alice@example.com",
                "hashedPassword"
        );

        userRepository.save(user);

        assertTrue(
                userRepository.existsByEmailIgnoreCase("ALICE@EXAMPLE.COM")
        );
    }


    @Test
    void existsByEmailIgnoreCase_returnsFalse_whenEmailDoesNotExist() {
        assertFalse(
                userRepository.existsByEmailIgnoreCase("missing@example.com")
        );
    }


    @Test
    void save_throwsException_whenEmailIsDuplicated() {
        User first = User.register(
                "Alice",
                "Smith",
                "alice@example.com",
                "password"
        );

        User second = User.register(
                "Bob",
                "Brown",
                "bob@example.com",
                "password"
        );

        userRepository.saveAndFlush(first);

        second.setEmail("alice@example.com");

        assertThrows(
                Exception.class,
                () -> userRepository.saveAndFlush(second)
        );
    }


    @Test
    void save_throwsException_whenEmailDuplicateDiffersOnlyByCase() {
        User first = User.register(
                "Alice",
                "Smith",
                "alice@example.com",
                "password"
        );

        User second = User.register(
                "Bob",
                "Brown",
                "ALICE@EXAMPLE.COM",
                "password"
        );

        userRepository.saveAndFlush(first);

        assertThrows(
                Exception.class,
                () -> userRepository.saveAndFlush(second)
        );
    }

    @Test
    @DisplayName("countByRole counts only users with the given role")
    void countByRoleCountsAdmins() {
        long adminsBefore = userRepository.countByRole(UserRole.ADMIN);
        long usersBefore = userRepository.countByRole(UserRole.USER);

        User admin = User.register("Ada", "Admin", "count-admin@test.com", "hash");
        ReflectionTestUtils.setField(admin, "role", UserRole.ADMIN);
        User regular = User.register("Rita", "Regular", "count-user@test.com", "hash");

        userRepository.save(admin);
        userRepository.saveAndFlush(regular);

        assertEquals(adminsBefore + 1, userRepository.countByRole(UserRole.ADMIN));
        assertEquals(usersBefore + 1, userRepository.countByRole(UserRole.USER));
    }

    @Test
    @DisplayName("countByEnabledFalse counts only disabled accounts")
    void countByEnabledFalseCountsDisabled() {
        User enabled = User.register("Eve", "Enabled", "count-enabled@test.com", "hash");
        User disabled = User.register("Dan", "Disabled", "count-disabled@test.com", "hash");
        disabled.setEnabled(false);

        userRepository.save(enabled);
        userRepository.saveAndFlush(disabled);

        assertEquals(1, userRepository.countByEnabledFalse());
    }
}