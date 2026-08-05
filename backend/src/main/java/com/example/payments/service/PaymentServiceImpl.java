package com.example.payments.service;

import com.example.payments.dto.CreatePaymentRequest;
import com.example.payments.dto.CreatePaymentResult;
import com.example.payments.exception.ConflictException;
import com.example.payments.exception.ForbiddenException;
import com.example.payments.exception.GoneException;
import com.example.payments.exception.InvalidStatusTransitionException;
import com.example.payments.exception.PaymentNotFoundException;
import com.example.payments.exception.ResourceNotFoundException;
import com.example.payments.exception.ValidationException;
import com.example.payments.model.ErrorCode;
import com.example.payments.model.PaymentIntentStatus;
import com.example.payments.model.PaymentStatus;
import com.example.payments.model.RoleType;
import com.example.payments.model.entity.Account;
import com.example.payments.model.entity.Payment;
import com.example.payments.model.entity.PaymentIntent;
import com.example.payments.model.entity.PaymentSendAttempt;
import com.example.payments.model.entity.PaymentStatusHistory;
import com.example.payments.model.entity.Payee;
import com.example.payments.model.entity.User;
import com.example.payments.repository.AccountRepository;
import com.example.payments.repository.PaymentRepository;
import com.example.payments.repository.PaymentIntentRepository;
import com.example.payments.repository.PaymentSendAttemptRepository;
import com.example.payments.repository.PaymentStatusHistoryRepository;
import com.example.payments.repository.PayeeRepository;
import com.example.payments.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentServiceImpl implements PaymentService {

    private static final Set<String> SUPPORTED_CURRENCIES = Set.of("USD", "EUR", "GBP", "INR");

    private static final Map<PaymentStatus, Set<PaymentStatus>> ALLOWED_TRANSITIONS = new EnumMap<>(PaymentStatus.class);

    static {
        ALLOWED_TRANSITIONS.put(PaymentStatus.CREATED, Set.of(PaymentStatus.VALIDATED, PaymentStatus.FAILED));
        ALLOWED_TRANSITIONS.put(PaymentStatus.VALIDATED, Set.of(PaymentStatus.SENT, PaymentStatus.FAILED));
        ALLOWED_TRANSITIONS.put(PaymentStatus.SENT, Set.of(PaymentStatus.COMPLETED, PaymentStatus.FAILED));
        ALLOWED_TRANSITIONS.put(PaymentStatus.COMPLETED, Set.of());
        ALLOWED_TRANSITIONS.put(PaymentStatus.FAILED, Set.of());
    }

    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final PayeeRepository payeeRepository;
    private final PaymentIntentRepository paymentIntentRepository;
    private final PaymentSendAttemptRepository paymentSendAttemptRepository;
    private final PaymentStatusHistoryRepository historyRepository;
    private final RetrySendService retrySendService;
    private final CurrencyConversionService currencyConversionService;
    private final BigDecimal maxTransactionAmount;
    private final long intentExpiryMinutes;
    private final String defaultSourceAccount;

    public PaymentServiceImpl(
        PaymentRepository paymentRepository,
        UserRepository userRepository,
        AccountRepository accountRepository,
        PayeeRepository payeeRepository,
        PaymentIntentRepository paymentIntentRepository,
        PaymentSendAttemptRepository paymentSendAttemptRepository,
        PaymentStatusHistoryRepository historyRepository,
        RetrySendService retrySendService,
        CurrencyConversionService currencyConversionService,
        @Value("${payment.limits.max-transaction-amount:10000}") BigDecimal maxTransactionAmount,
        @Value("${payment.intent.expiry-minutes:30}") long intentExpiryMinutes,
        @Value("${payment.single-user.account-number:ACC-001}") String defaultSourceAccount
    ) {
        this.paymentRepository = paymentRepository;
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.payeeRepository = payeeRepository;
        this.paymentIntentRepository = paymentIntentRepository;
        this.paymentSendAttemptRepository = paymentSendAttemptRepository;
        this.historyRepository = historyRepository;
        this.retrySendService = retrySendService;
        this.currencyConversionService = currencyConversionService;
        this.maxTransactionAmount = maxTransactionAmount;
        this.intentExpiryMinutes = intentExpiryMinutes;
        this.defaultSourceAccount = defaultSourceAccount;
    }

    @Override
    @Transactional
    public CreatePaymentResult createPayment(Long userId, String idempotencyKey, CreatePaymentRequest request) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new ValidationException(ErrorCode.VALIDATION_FAILED, "Idempotency-Key header is required");
        }

        if (request != null && isBlank(request.getSourceAccount())) {
            request.setSourceAccount(defaultSourceAccount);
        }

        validateCreateRequest(userId, request);

        LocalDateTime now = LocalDateTime.now();
        Payment payment = new Payment();
        payment.setSourceAccount(request.getSourceAccount().trim());
        payment.setDestAccount(request.getDestAccount().trim());
        payment.setAmount(request.getAmount());
        payment.setCurrency(request.getCurrency().trim().toUpperCase());
        payment.setReference(request.getReference());
        payment.setStatus(PaymentStatus.CREATED);
        payment.setIdempotencyKey(idempotencyKey);
        payment.setCreatedByUser(resolveUser(userId));
        payment.setVersion(0);
        payment.setCreatedAt(now);
        payment.setUpdatedAt(now);

        PaymentIntent intent = null;
        if (request.getPaymentIntentId() != null) {
            intent = resolveIntentForConversion(userId, request.getPaymentIntentId());
            if (!intent.getPayeeAccountNumber().equals(payment.getDestAccount())) {
                throw new ValidationException(
                    ErrorCode.VALIDATION_FAILED,
                    "Destination account must match payment intent payee account"
                );
            }
            payment.setPaymentIntent(intent);
        }

        Payment saved;
        boolean created = true;
        try {
            saved = paymentRepository.save(payment);
        } catch (DataIntegrityViolationException ex) {
            Payment existing = paymentRepository
                .findByIdempotencyKey(idempotencyKey)
                .orElseThrow(() -> new ConflictException(ErrorCode.IDEMPOTENCY_KEY_REUSED, "Idempotency key already used"));
            ensureSamePayloadOrThrow(existing, request, userId);
            return new CreatePaymentResult(existing, false);
        }

        saveHistory(saved, null, PaymentStatus.CREATED, "Payment created");

        if (intent != null) {
            intent.setStatus(PaymentIntentStatus.CONVERTED);
            paymentIntentRepository.save(intent);
        }

        return new CreatePaymentResult(saved, created);
    }

    @Override
    @Transactional(readOnly = true)
    public Payment getPayment(Long userId, Long id) {
        Payment payment = paymentRepository.findById(id).orElseThrow(() -> new PaymentNotFoundException(id));
        enforcePaymentVisibility(userId, payment);
        return payment;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Payment> listPayments(Long userId, String account, RoleType role, PaymentStatus status) {
        if (role == RoleType.SENT && (account == null || account.isBlank())) {
            return status == null
                ? paymentRepository.findByCreatedByUser_IdOrderByCreatedAtDesc(userId)
                : paymentRepository.findByCreatedByUser_IdAndStatusOrderByCreatedAtDesc(userId, status);
        }

        List<String> ownedAccounts = accountRepository
            .findByUser_IdOrderByAccountNumberAsc(userId)
            .stream()
            .map(Account::getAccountNumber)
            .collect(Collectors.toList());

        if (ownedAccounts.isEmpty()) {
            return List.of();
        }

        if (account != null && !account.isBlank() && !ownedAccounts.contains(account.trim())) {
            throw new ForbiddenException(ErrorCode.INVALID_ACCOUNT_OWNERSHIP, "Selected account does not belong to user");
        }

        List<String> filterAccounts = account == null || account.isBlank() ? ownedAccounts : List.of(account.trim());
        if (role == RoleType.SENT) {
            return status == null
                ? paymentRepository.findBySourceAccountInOrderByCreatedAtDesc(filterAccounts)
                : paymentRepository.findBySourceAccountInAndStatusOrderByCreatedAtDesc(filterAccounts, status);
        }

        return status == null
            ? paymentRepository.findByDestAccountInOrderByCreatedAtDesc(filterAccounts)
            : paymentRepository.findByDestAccountInAndStatusOrderByCreatedAtDesc(filterAccounts, status);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentStatusHistory> getPaymentHistory(Long userId, Long id) {
        Payment payment = getPayment(userId, id);
        return historyRepository.findByPaymentIdOrderByChangedAtAsc(payment.getId());
    }

    @Override
    @Transactional
    public Payment transitionStatus(Long userId, Long paymentId, PaymentStatus newStatus, String reason) {
        Payment payment = getPayment(userId, paymentId);
        PaymentStatus currentStatus = payment.getStatus();

        Set<PaymentStatus> allowedStatuses = ALLOWED_TRANSITIONS.get(currentStatus);
        if (!allowedStatuses.contains(newStatus)) {
            throw new InvalidStatusTransitionException(currentStatus, newStatus);
        }

        if (currentStatus == PaymentStatus.CREATED && newStatus == PaymentStatus.VALIDATED) {
            String businessError = businessRuleCheck(payment);
            if (businessError != null) {
                return transitionWithOptimisticLock(payment, PaymentStatus.FAILED, businessError);
            }
        }

        return transitionWithOptimisticLock(payment, newStatus, reason);
    }

    @Override
    @Transactional
    public void sendPayment(Long userId, Long paymentId) {
        Payment payment = getPayment(userId, paymentId);
        if (payment.getStatus() != PaymentStatus.VALIDATED) {
            throw new InvalidStatusTransitionException(payment.getStatus(), PaymentStatus.SENT);
        }
        retrySendService.attemptSend(paymentId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentSendAttempt> getSendAttempts(Long userId, Long paymentId) {
        Payment payment = getPayment(userId, paymentId);
        return paymentSendAttemptRepository.findByPaymentIdOrderByAttemptNumberAsc(payment.getId());
    }

    private String businessRuleCheck(Payment payment) {
        BigDecimal amountInBaseCurrency = currencyConversionService.convertToBase(payment.getAmount(), payment.getCurrency());
        if (amountInBaseCurrency.compareTo(maxTransactionAmount) > 0) {
            return ErrorCode.LIMIT_EXCEEDED.name();
        }

        Account sourceAccount = accountRepository
            .findById(payment.getSourceAccount())
            .orElseGet(() -> createSimulationSourceAccount(payment));

        if (amountInBaseCurrency.compareTo(sourceAccount.getBalance()) > 0) {
            return ErrorCode.INSUFFICIENT_FUNDS.name();
        }

        sourceAccount.setBalance(sourceAccount.getBalance().subtract(amountInBaseCurrency));
        accountRepository.save(sourceAccount);
        return null;
    }

    private Account createSimulationSourceAccount(Payment payment) {
        Account account = new Account();
        account.setAccountNumber(payment.getSourceAccount());
        account.setUser(resolveUser(payment.getCreatedByUser().getId()));
        account.setDisplayName("Simulated account");
        account.setBalance(new BigDecimal("50000.00"));
        account.setCreatedAt(LocalDateTime.now());

        try {
            return accountRepository.save(account);
        } catch (DataIntegrityViolationException ex) {
            return accountRepository
                .findById(payment.getSourceAccount())
                .orElseThrow(() -> new ValidationException(ErrorCode.PROCESSING_ERROR, "Unable to initialize source account"));
        }
    }

    private Payment transitionWithOptimisticLock(Payment payment, PaymentStatus newStatus, String reason) {
        PaymentStatus currentStatus = payment.getStatus();
        int rows = paymentRepository.compareAndSwapStatus(
            payment.getId(),
            currentStatus,
            payment.getVersion(),
            newStatus,
            newStatus == PaymentStatus.FAILED
                ? (reason == null || reason.isBlank() ? ErrorCode.PROCESSING_ERROR.name() : reason)
                : null,
            LocalDateTime.now()
        );
        if (rows == 0) {
            throw new ConflictException(ErrorCode.CONCURRENT_UPDATE, "Payment was updated concurrently");
        }

        Payment updated = paymentRepository.findById(payment.getId()).orElseThrow(() -> new PaymentNotFoundException(payment.getId()));
        saveHistory(updated, currentStatus, newStatus, reason);
        return updated;
    }

    private PaymentIntent resolveIntentForConversion(Long userId, Long intentId) {
        PaymentIntent intent = paymentIntentRepository
            .findByIdAndInitiatedByUser_Id(intentId, userId)
            .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.INTENT_NOT_FOUND, "Payment intent not found"));

        if (intent.getStatus() == PaymentIntentStatus.CONVERTED) {
            throw new ConflictException(ErrorCode.INTENT_ALREADY_CONVERTED, "Payment intent already converted");
        }

        if (intent.getStatus() == PaymentIntentStatus.EXPIRED) {
            throw new GoneException(ErrorCode.INTENT_EXPIRED, "Payment intent has expired");
        }

        if (intent.getCreatedAt().plusMinutes(intentExpiryMinutes).isBefore(LocalDateTime.now())) {
            intent.setStatus(PaymentIntentStatus.EXPIRED);
            paymentIntentRepository.save(intent);
            throw new GoneException(ErrorCode.INTENT_EXPIRED, "Payment intent has expired");
        }

        return intent;
    }

    private void ensureSamePayloadOrThrow(Payment existing, CreatePaymentRequest request, Long userId) {
        boolean samePayload = Objects.equals(existing.getSourceAccount(), request.getSourceAccount().trim()) &&
            Objects.equals(existing.getDestAccount(), request.getDestAccount().trim()) &&
            existing.getAmount().compareTo(request.getAmount()) == 0 &&
            Objects.equals(existing.getCurrency(), request.getCurrency().trim().toUpperCase()) &&
            Objects.equals(existing.getReference(), request.getReference()) &&
            Objects.equals(existing.getCreatedByUser().getId(), userId);
        if (!samePayload) {
            throw new ConflictException(ErrorCode.IDEMPOTENCY_KEY_REUSED, "Idempotency key reused with different payload");
        }
    }

    private void enforcePaymentVisibility(Long userId, Payment payment) {
        // Simulation mode: disable ownership visibility checks.
        // Any payment can be viewed/transitioned by the single local user context.
    }

    private User resolveUser(Long userId) {
        return userRepository
            .findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.UNAUTHORIZED, "Authenticated user not found"));
    }

    private void validateCreateRequest(Long userId, CreatePaymentRequest request) {
        if (request == null) {
            throw new ValidationException(ErrorCode.VALIDATION_FAILED, "Request body is required");
        }

        if (isBlank(request.getSourceAccount()) || isBlank(request.getDestAccount())) {
            throw new ValidationException(ErrorCode.INVALID_ACCOUNT, "Source and destination accounts are required");
        }

        if (request.getSourceAccount().trim().equals(request.getDestAccount().trim())) {
            throw new ValidationException(ErrorCode.INVALID_ACCOUNT, "Source and destination accounts must differ");
        }

        BigDecimal amount = request.getAmount();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException(ErrorCode.INVALID_AMOUNT, "Amount must be greater than 0");
        }

        if (amount.scale() > 2) {
            throw new ValidationException(ErrorCode.INVALID_AMOUNT, "Amount cannot have more than 2 decimal places");
        }

        String currency = request.getCurrency();
        if (isBlank(currency) || !SUPPORTED_CURRENCIES.contains(currency.trim().toUpperCase())) {
            throw new ValidationException(ErrorCode.INVALID_CURRENCY, "Currency must be one of USD, EUR, GBP, INR");
        }

        if (request.getPaymentIntentId() != null) {
            PaymentIntent intent = paymentIntentRepository
                .findByIdAndInitiatedByUser_Id(request.getPaymentIntentId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.INTENT_NOT_FOUND, "Payment intent not found"));
            if (intent.getStatus() == PaymentIntentStatus.CONVERTED) {
                throw new ConflictException(ErrorCode.INTENT_ALREADY_CONVERTED, "Intent already converted");
            }
        }

        payeeRepository.findByOwnerUser_IdAndPayeeAccountNumber(userId, request.getDestAccount().trim()).orElse(null);
    }

    private void saveHistory(Payment payment, PaymentStatus fromStatus, PaymentStatus toStatus, String reason) {
        PaymentStatusHistory history = new PaymentStatusHistory();
        history.setPayment(payment);
        history.setFromStatus(fromStatus);
        history.setToStatus(toStatus);
        history.setChangedAt(LocalDateTime.now());
        history.setReason(reason);
        historyRepository.save(history);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
