package com.example.payments.security;

import com.example.payments.model.entity.Account;
import com.example.payments.model.entity.User;
import com.example.payments.repository.AccountRepository;
import com.example.payments.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CurrentUserService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final String singleUserEmail;
    private final String singleUserFullName;
    private final String singleUserAccountNumber;

    private volatile Long cachedUserId;

    public CurrentUserService(
        UserRepository userRepository,
        AccountRepository accountRepository,
        @Value("${payment.single-user.email:single.user@local}") String singleUserEmail,
        @Value("${payment.single-user.full-name:Single User}") String singleUserFullName,
        @Value("${payment.single-user.account-number:ACC-001}") String singleUserAccountNumber
    ) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.singleUserEmail = singleUserEmail;
        this.singleUserFullName = singleUserFullName;
        this.singleUserAccountNumber = singleUserAccountNumber;
    }

    public Long getCurrentUserId() {
        Long userId = cachedUserId;
        if (userId != null) {
            return userId;
        }

        synchronized (this) {
            if (cachedUserId == null) {
                cachedUserId = resolveOrCreateSingleUser().getId();
            }
            return cachedUserId;
        }
    }

    @Transactional
    protected User resolveOrCreateSingleUser() {
        User user = userRepository.findByEmail(singleUserEmail).orElseGet(() -> {
            User newUser = new User();
            newUser.setEmail(singleUserEmail);
            newUser.setFullName(singleUserFullName);
            newUser.setPasswordHash("single-user-no-login");
            newUser.setCreatedAt(LocalDateTime.now());
            return userRepository.save(newUser);
        });

        if (!accountRepository.existsById(singleUserAccountNumber)) {
            Account account = new Account();
            account.setAccountNumber(singleUserAccountNumber);
            account.setUser(user);
            account.setDisplayName(singleUserFullName);
            account.setBalance(new BigDecimal("50000.00"));
            account.setCreatedAt(LocalDateTime.now());
            accountRepository.save(account);
        }

        return user;
    }
}
