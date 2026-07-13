package com.borrowly.repository.user;

import com.borrowly.model.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class UserRepositoryTest {

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
}