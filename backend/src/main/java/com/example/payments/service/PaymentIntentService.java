package com.example.payments.service;

import com.example.payments.dto.PaymentIntentRequest;
import com.example.payments.exception.ConflictException;
import com.example.payments.exception.GoneException;
import com.example.payments.exception.ResourceNotFoundException;
import com.example.payments.exception.ValidationException;
import com.example.payments.model.ErrorCode;
import com.example.payments.model.PaymentIntentStatus;
import com.example.payments.model.entity.Payee;
import com.example.payments.model.entity.PaymentIntent;
import com.example.payments.model.entity.User;
import com.example.payments.repository.AccountRepository;
import com.example.payments.repository.PayeeRepository;
import com.example.payments.repository.PaymentIntentRepository;
import com.example.payments.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentIntentService {

    private final PaymentIntentRepository paymentIntentRepository;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final PayeeRepository payeeRepository;
    private final long expiryMinutes;

    public PaymentIntentService(
        PaymentIntentRepository paymentIntentRepository,
        AccountRepository accountRepository,
        UserRepository userRepository,
        PayeeRepository payeeRepository,
        @Value("${payment.intent.expiry-minutes:30}") long expiryMinutes
    ) {
        this.paymentIntentRepository = paymentIntentRepository;
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
        this.payeeRepository = payeeRepository;
        this.expiryMinutes = expiryMinutes;
    }

    @Transactional
    public PaymentIntent createIntent(Long userId, PaymentIntentRequest request) {
        if (request == null || request.getPayeeAccountNumber() == null || request.getPayeeAccountNumber().isBlank()) {
            throw new ValidationException(ErrorCode.VALIDATION_FAILED, "payeeAccountNumber is required");
        }

        String payeeAccount = request.getPayeeAccountNumber().trim();
        User user = userRepository
            .findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.UNAUTHORIZED, "Authenticated user not found"));

        PaymentIntent intent = new PaymentIntent();
        intent.setIdempotencyKey(UUID.randomUUID().toString());
        intent.setInitiatedByUser(user);
        intent.setPayeeAccountNumber(payeeAccount);
        intent.setStatus(PaymentIntentStatus.CREATED);
        intent.setCreatedAt(LocalDateTime.now());
        return paymentIntentRepository.save(intent);
    }

    @Transactional
    public PaymentIntent getIntent(Long userId, Long intentId) {
        PaymentIntent intent = paymentIntentRepository
            .findByIdAndInitiatedByUser_Id(intentId, userId)
            .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.INTENT_NOT_FOUND, "Payment intent not found"));

        if (intent.getStatus() == PaymentIntentStatus.CONVERTED) {
            throw new ConflictException(ErrorCode.INTENT_ALREADY_CONVERTED, "Intent already converted");
        }

        if (intent.getStatus() == PaymentIntentStatus.EXPIRED) {
            throw new GoneException(ErrorCode.INTENT_EXPIRED, "Payment intent has expired");
        }

        if (intent.getCreatedAt().plusMinutes(expiryMinutes).isBefore(LocalDateTime.now())) {
            intent.setStatus(PaymentIntentStatus.EXPIRED);
            paymentIntentRepository.save(intent);
            throw new GoneException(ErrorCode.INTENT_EXPIRED, "Payment intent has expired");
        }

        return intent;
    }

    @Transactional(readOnly = true)
    public String resolveNickname(Long userId, String payeeAccountNumber) {
        Payee payee = payeeRepository.findByOwnerUser_IdAndPayeeAccountNumber(userId, payeeAccountNumber).orElse(null);
        return payee == null ? null : payee.getNickname();
    }
}
