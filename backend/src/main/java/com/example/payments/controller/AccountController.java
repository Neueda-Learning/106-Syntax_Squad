package com.example.payments.controller;

import com.example.payments.dto.AccountResponse;
import com.example.payments.repository.AccountRepository;
import com.example.payments.security.CurrentUserService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountRepository accountRepository;
    private final CurrentUserService currentUserService;

    public AccountController(AccountRepository accountRepository, CurrentUserService currentUserService) {
        this.accountRepository = accountRepository;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/me")
    public List<AccountResponse> myAccounts() {
        Long userId = currentUserService.getCurrentUserId();
        return accountRepository
            .findByUser_IdOrderByAccountNumberAsc(userId)
            .stream()
            .map(account -> {
                AccountResponse response = new AccountResponse();
                response.setAccountNumber(account.getAccountNumber());
                response.setDisplayName(account.getDisplayName());
                response.setBalance(account.getBalance());
                return response;
            })
            .toList();
    }
}
