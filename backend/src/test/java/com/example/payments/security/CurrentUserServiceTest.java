package com.example.payments.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.payments.model.entity.User;
import com.example.payments.repository.AccountRepository;
import com.example.payments.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CurrentUserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AccountRepository accountRepository;

    private CurrentUserService currentUserService;

    @BeforeEach
    void setUp() {
        currentUserService = new CurrentUserService(
            userRepository,
            accountRepository,
            "single.user@local",
            "Single User",
            "ACC-001"
        );
    }

    @Test
    void returns_existing_single_user_id_without_creating_new_account() {
        User existing = new User();
        existing.setId(42L);
        existing.setEmail("single.user@local");

        when(userRepository.findByEmail("single.user@local")).thenReturn(Optional.of(existing));
        when(accountRepository.existsById("ACC-001")).thenReturn(true);

        Long userId = currentUserService.getCurrentUserId();

        assertEquals(42L, userId);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void caches_resolved_single_user_id() {
        User existing = new User();
        existing.setId(7L);
        existing.setEmail("single.user@local");

        when(userRepository.findByEmail("single.user@local")).thenReturn(Optional.of(existing));
        when(accountRepository.existsById("ACC-001")).thenReturn(true);

        Long firstCall = currentUserService.getCurrentUserId();
        Long secondCall = currentUserService.getCurrentUserId();

        assertEquals(7L, firstCall);
        assertEquals(7L, secondCall);
        verify(userRepository).findByEmail("single.user@local");
    }
}
