package com.example.payments.service;

import com.example.payments.dto.PayeeRequest;
import com.example.payments.exception.ForbiddenException;
import com.example.payments.exception.ResourceNotFoundException;
import com.example.payments.exception.ValidationException;
import com.example.payments.model.ErrorCode;
import com.example.payments.model.entity.Payee;
import com.example.payments.repository.AccountRepository;
import com.example.payments.repository.PayeeRepository;
import com.example.payments.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PayeeService {

    private final PayeeRepository payeeRepository;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    public PayeeService(PayeeRepository payeeRepository, AccountRepository accountRepository, UserRepository userRepository) {
        this.payeeRepository = payeeRepository;
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Payee createPayee(Long userId, PayeeRequest request) {
        if (request == null || isBlank(request.getPayeeAccountNumber()) || isBlank(request.getNickname())) {
            throw new ValidationException(ErrorCode.VALIDATION_FAILED, "payeeAccountNumber and nickname are required");
        }

        String payeeAccount = request.getPayeeAccountNumber().trim();
        if (accountRepository.existsByAccountNumberAndUser_Id(payeeAccount, userId)) {
            throw new ValidationException(ErrorCode.INVALID_ACCOUNT, "Cannot add your own account as payee");
        }

        Payee existing = payeeRepository.findByOwnerUser_IdAndPayeeAccountNumber(userId, payeeAccount).orElse(null);
        if (existing != null) {
            return existing;
        }

        Payee payee = new Payee();
        payee.setOwnerUser(userRepository.getReferenceById(userId));
        payee.setPayeeAccountNumber(payeeAccount);
        payee.setNickname(request.getNickname().trim());
        payee.setCreatedAt(LocalDateTime.now());
        return payeeRepository.save(payee);
    }

    @Transactional(readOnly = true)
    public List<Payee> listPayees(Long userId) {
        return payeeRepository.findByOwnerUser_IdOrderByCreatedAtDesc(userId);
    }

    @Transactional
    public void deletePayee(Long userId, Long payeeId) {
        Payee payee = payeeRepository
            .findById(payeeId)
            .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.PAYEE_NOT_FOUND, "Payee not found"));

        if (!payee.getOwnerUser().getId().equals(userId)) {
            throw new ForbiddenException(ErrorCode.INVALID_ACCOUNT_OWNERSHIP, "Cannot delete payee from another user");
        }

        payeeRepository.delete(payee);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
