package com.example.payments.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.payments.dto.AccountResponse;
import com.example.payments.model.entity.Account;
import com.example.payments.repository.AccountRepository;
import com.example.payments.security.CurrentUserService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AccountControllerTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private CurrentUserService currentUserService;

    private AccountController accountController;

    @BeforeEach
    void setUp() {
        accountController = new AccountController(accountRepository, currentUserService);
    }

    @Test
    void returns_accounts_for_current_user_with_mapped_fields() {
        Account account = new Account();
        account.setAccountNumber("ACC-101");
        account.setDisplayName("Main Wallet");
        account.setBalance(new BigDecimal("1250.75"));

        when(currentUserService.getCurrentUserId()).thenReturn(9L);
        when(accountRepository.findByUser_IdOrderByAccountNumberAsc(9L)).thenReturn(List.of(account));

        List<AccountResponse> result = accountController.myAccounts();

        assertEquals(1, result.size());
        assertEquals("ACC-101", result.get(0).getAccountNumber());
        assertEquals("Main Wallet", result.get(0).getDisplayName());
        assertEquals(new BigDecimal("1250.75"), result.get(0).getBalance());
        verify(accountRepository).findByUser_IdOrderByAccountNumberAsc(9L);
    }

    @Test
    void returns_empty_list_when_current_user_has_no_accounts() {
        when(currentUserService.getCurrentUserId()).thenReturn(15L);
        when(accountRepository.findByUser_IdOrderByAccountNumberAsc(15L)).thenReturn(List.of());

        List<AccountResponse> result = accountController.myAccounts();

        assertTrue(result.isEmpty());
        verify(accountRepository).findByUser_IdOrderByAccountNumberAsc(15L);
    }
}

