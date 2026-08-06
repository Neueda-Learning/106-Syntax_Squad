package com.example.payments.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.payments.model.entity.Account;
import com.example.payments.model.entity.User;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AccountRepositoryTest extends AbstractRepositoryTest {

    private AccountRepository accountRepository;
    private UserRepository userRepository;
    private User owner;

    @BeforeEach
    void setUp() {
        accountRepository = new AccountRepository(jdbcTemplate);
        userRepository = new UserRepository(jdbcTemplate);

        User user = new User();
        user.setEmail("owner@example.com");
        user.setPasswordHash("hash");
        user.setFullName("Owner");
        user.setCreatedAt(LocalDateTime.now());
        owner = userRepository.save(user);
    }

    private Account newAccount(String accountNumber, BigDecimal balance) {
        Account account = new Account();
        account.setAccountNumber(accountNumber);
        account.setUser(owner);
        account.setDisplayName("Display " + accountNumber);
        account.setBalance(balance);
        account.setCreatedAt(LocalDateTime.now());
        return account;
    }

    @Test
    void save_insertsNewAccount() {
        accountRepository.save(newAccount("ACC-001", new BigDecimal("50000.00")));

        Optional<Account> found = accountRepository.findById("ACC-001");

        assertTrue(found.isPresent());
        assertEquals(0, new BigDecimal("50000.00").compareTo(found.get().getBalance()));
        assertEquals(owner.getId(), found.get().getUser().getId());
    }

    @Test
    void save_upsertsExistingAccount() {
        accountRepository.save(newAccount("ACC-002", new BigDecimal("50000.00")));

        accountRepository.save(newAccount("ACC-002", new BigDecimal("100.00")));

        Optional<Account> found = accountRepository.findById("ACC-002");
        assertTrue(found.isPresent());
        assertEquals(0, new BigDecimal("100.00").compareTo(found.get().getBalance()));
    }

    @Test
    void findById_returnsEmpty_whenMissing() {
        assertTrue(accountRepository.findById("MISSING").isEmpty());
    }

    @Test
    void existsById_reflectsPresence() {
        accountRepository.save(newAccount("ACC-003", new BigDecimal("10.00")));

        assertTrue(accountRepository.existsById("ACC-003"));
        assertFalse(accountRepository.existsById("MISSING"));
    }

    @Test
    void existsByAccountNumberAndUser_Id_reflectsOwnership() {
        accountRepository.save(newAccount("ACC-004", new BigDecimal("10.00")));

        assertTrue(accountRepository.existsByAccountNumberAndUser_Id("ACC-004", owner.getId()));
        assertFalse(accountRepository.existsByAccountNumberAndUser_Id("ACC-004", 999L));
    }

    @Test
    void findByUser_IdOrderByAccountNumberAsc_returnsSortedAccounts() {
        accountRepository.save(newAccount("ACC-020", new BigDecimal("10.00")));
        accountRepository.save(newAccount("ACC-010", new BigDecimal("20.00")));

        List<Account> accounts = accountRepository.findByUser_IdOrderByAccountNumberAsc(owner.getId());

        assertEquals(2, accounts.size());
        assertEquals("ACC-010", accounts.get(0).getAccountNumber());
        assertEquals("ACC-020", accounts.get(1).getAccountNumber());
    }
}
