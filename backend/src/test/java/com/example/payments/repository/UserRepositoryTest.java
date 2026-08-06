package com.example.payments.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.payments.model.entity.User;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UserRepositoryTest extends AbstractRepositoryTest {

    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository = new UserRepository(jdbcTemplate);
    }

    private User newUser(String email) {
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash("hash");
        user.setFullName("Test User");
        user.setCreatedAt(LocalDateTime.now());
        return user;
    }

    @Test
    void save_assignsGeneratedId() {
        User saved = userRepository.save(newUser("alice@example.com"));

        assertTrue(saved.getId() > 0);
    }

    @Test
    void findByEmail_returnsUser_whenExists() {
        userRepository.save(newUser("bob@example.com"));

        Optional<User> found = userRepository.findByEmail("bob@example.com");

        assertTrue(found.isPresent());
        assertEquals("bob@example.com", found.get().getEmail());
        assertEquals("Test User", found.get().getFullName());
    }

    @Test
    void findByEmail_returnsEmpty_whenMissing() {
        Optional<User> found = userRepository.findByEmail("missing@example.com");

        assertTrue(found.isEmpty());
    }

    @Test
    void findById_returnsUser_whenExists() {
        User saved = userRepository.save(newUser("carol@example.com"));

        Optional<User> found = userRepository.findById(saved.getId());

        assertTrue(found.isPresent());
        assertEquals(saved.getId(), found.get().getId());
    }

    @Test
    void findById_returnsEmpty_whenMissing() {
        assertTrue(userRepository.findById(999L).isEmpty());
    }

    @Test
    void existsByEmail_reflectsPresence() {
        userRepository.save(newUser("dave@example.com"));

        assertTrue(userRepository.existsByEmail("dave@example.com"));
        assertFalse(userRepository.existsByEmail("nobody@example.com"));
    }

    @Test
    void getReferenceById_returnsUser_whenExists() {
        User saved = userRepository.save(newUser("erin@example.com"));

        User reference = userRepository.getReferenceById(saved.getId());

        assertEquals(saved.getId(), reference.getId());
    }

    @Test
    void getReferenceById_throws_whenMissing() {
        assertThrows(IllegalArgumentException.class, () -> userRepository.getReferenceById(999L));
    }
}
