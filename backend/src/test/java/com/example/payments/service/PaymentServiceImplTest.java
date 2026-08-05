package com.example.payments.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.payments.dto.CreatePaymentRequest;
import com.example.payments.dto.CreatePaymentResult;
import com.example.payments.exception.ConflictException;
import com.example.payments.exception.GoneException;
import com.example.payments.model.ErrorCode;
import com.example.payments.model.PaymentIntentStatus;
import com.example.payments.model.PaymentStatus;
import com.example.payments.model.entity.Account;
import com.example.payments.model.entity.Payment;
import com.example.payments.model.entity.PaymentIntent;
import com.example.payments.model.entity.User;
import com.example.payments.repository.AccountRepository;
import com.example.payments.repository.PaymentIntentRepository;
import com.example.payments.repository.PaymentRepository;
import com.example.payments.repository.PaymentSendAttemptRepository;
import com.example.payments.repository.PaymentStatusHistoryRepository;
import com.example.payments.repository.PayeeRepository;
import com.example.payments.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private PayeeRepository payeeRepository;

    @Mock
    private PaymentIntentRepository paymentIntentRepository;

    @Mock
    private PaymentSendAttemptRepository paymentSendAttemptRepository;

    @Mock
    private PaymentStatusHistoryRepository historyRepository;

    private PaymentServiceImpl paymentService;
    private CurrencyConversionService currencyConversionService;

    @BeforeEach
    void setUp() {
        currencyConversionService = new CurrencyConversionService(
            "USD",
            new BigDecimal("1.0"),
            new BigDecimal("1.08"),
            new BigDecimal("1.27"),
            new BigDecimal("0.012")
        );

        RetrySendService retrySendService = new RetrySendService(
            paymentRepository,
            paymentSendAttemptRepository,
            historyRepository,
            3,
            1000,
            0.8
        );

        paymentService = new PaymentServiceImpl(
            paymentRepository,
            userRepository,
            accountRepository,
            payeeRepository,
            paymentIntentRepository,
            paymentSendAttemptRepository,
            historyRepository,
            retrySendService,
            currencyConversionService,
            new BigDecimal("10000"),
            30,
            "ACC-001"
        );
    }

    @Test
    void gate3_allows_source_account_without_ownership_check() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            payment.setId(101L);
            return payment;
        });

        CreatePaymentResult result = paymentService.createPayment(1L, "idem-1", validCreateRequest());

        assertTrue(result.isCreated());
        assertEquals(101L, result.getPayment().getId());
    }

    @Test
    void gate2_rejects_missing_idempotency_key() {
        com.example.payments.exception.ValidationException exception = assertThrows(
            com.example.payments.exception.ValidationException.class,
            () -> paymentService.createPayment(1L, " ", validCreateRequest())
        );

        assertEquals(ErrorCode.VALIDATION_FAILED, exception.getErrorCode());
    }

    @Test
    void create_payment_accepts_inr_currency() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            payment.setId(102L);
            return payment;
        });

        CreatePaymentRequest request = validCreateRequest();
        request.setCurrency("inr");

        CreatePaymentResult result = paymentService.createPayment(1L, "idem-inr", request);

        assertTrue(result.isCreated());
        assertEquals("INR", result.getPayment().getCurrency());
    }

    @Test
    void gate2_returns_existing_payment_for_duplicate_same_payload() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
        when(paymentRepository.save(any(Payment.class))).thenThrow(new DataIntegrityViolationException("duplicate"));
        when(paymentRepository.findByIdempotencyKey("idem-1")).thenReturn(Optional.of(existingPayment()));

        CreatePaymentResult result = paymentService.createPayment(1L, "idem-1", validCreateRequest());

        assertFalse(result.isCreated());
        assertEquals(99L, result.getPayment().getId());
    }

    @Test
    void gate2_rejects_reused_key_with_different_payload() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
        when(paymentRepository.save(any(Payment.class))).thenThrow(new DataIntegrityViolationException("duplicate"));

        Payment differentPayload = existingPayment();
        differentPayload.setAmount(new BigDecimal("999.00"));
        when(paymentRepository.findByIdempotencyKey("idem-1")).thenReturn(Optional.of(differentPayload));

        ConflictException exception = assertThrows(
            ConflictException.class,
            () -> paymentService.createPayment(1L, "idem-1", validCreateRequest())
        );

        assertEquals(ErrorCode.IDEMPOTENCY_KEY_REUSED, exception.getErrorCode());
    }

    @Test
    void concurrent_update_returns_conflict() {
        Payment payment = paymentWithStatus(PaymentStatus.VALIDATED);
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
        when(paymentRepository.compareAndSwapStatus(any(), any(), any(), any(), any(), any())).thenReturn(0);

        ConflictException exception = assertThrows(
            ConflictException.class,
            () -> paymentService.transitionStatus(1L, 1L, PaymentStatus.SENT, "manual")
        );

        assertEquals(ErrorCode.CONCURRENT_UPDATE, exception.getErrorCode());
    }

    @Test
    void gate4_limit_exceeded_transitions_to_failed() {
        Payment payment = paymentWithStatus(PaymentStatus.CREATED);
        payment.setAmount(new BigDecimal("20000.00"));

        Payment failed = paymentWithStatus(PaymentStatus.FAILED);
        failed.setErrorCode(ErrorCode.LIMIT_EXCEEDED.name());

        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment), Optional.of(failed));
        when(paymentRepository.compareAndSwapStatus(any(), any(), any(), any(), any(), any())).thenReturn(1);

        Payment transitioned = paymentService.transitionStatus(1L, 1L, PaymentStatus.VALIDATED, "manual validation");

        assertEquals(PaymentStatus.FAILED, transitioned.getStatus());
        assertEquals(ErrorCode.LIMIT_EXCEEDED.name(), transitioned.getErrorCode());
    }

    @Test
    void gate4_insufficient_funds_transitions_to_failed() {
        Payment payment = paymentWithStatus(PaymentStatus.CREATED);
        payment.setAmount(new BigDecimal("500.00"));

        Account source = new Account();
        source.setAccountNumber("ACC-100");
        source.setBalance(new BigDecimal("100.00"));

        Payment failed = paymentWithStatus(PaymentStatus.FAILED);
        failed.setErrorCode(ErrorCode.INSUFFICIENT_FUNDS.name());

        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment), Optional.of(failed));
        when(accountRepository.findById("ACC-100")).thenReturn(Optional.of(source));
        when(paymentRepository.compareAndSwapStatus(any(), any(), any(), any(), any(), any())).thenReturn(1);

        Payment transitioned = paymentService.transitionStatus(1L, 1L, PaymentStatus.VALIDATED, "manual validation");

        assertEquals(PaymentStatus.FAILED, transitioned.getStatus());
        assertEquals(ErrorCode.INSUFFICIENT_FUNDS.name(), transitioned.getErrorCode());
    }

    @Test
    void gate4_deducts_inr_using_base_currency_conversion() {
        Payment payment = paymentWithStatus(PaymentStatus.CREATED);
        payment.setAmount(new BigDecimal("1000.00"));
        payment.setCurrency("INR");

        Account source = new Account();
        source.setAccountNumber("ACC-100");
        source.setBalance(new BigDecimal("20.00"));

        Payment validated = paymentWithStatus(PaymentStatus.VALIDATED);

        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment), Optional.of(validated));
        when(accountRepository.findById("ACC-100")).thenReturn(Optional.of(source));
        when(paymentRepository.compareAndSwapStatus(any(), any(), any(), any(), any(), any())).thenReturn(1);

        Payment transitioned = paymentService.transitionStatus(1L, 1L, PaymentStatus.VALIDATED, "manual validation");

        ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(accountCaptor.capture());
        assertEquals(new BigDecimal("8.00"), accountCaptor.getValue().getBalance());
        assertEquals(PaymentStatus.VALIDATED, transitioned.getStatus());
    }

    @Test
    void intent_expired_returns_gone() {
        CreatePaymentRequest request = validCreateRequest();
        request.setPaymentIntentId(12L);

        PaymentIntent expired = new PaymentIntent();
        expired.setId(12L);
        expired.setPayeeAccountNumber("ACC-200");
        expired.setStatus(PaymentIntentStatus.CREATED);
        expired.setCreatedAt(LocalDateTime.now().minusMinutes(45));

        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
        when(paymentIntentRepository.findByIdAndInitiatedByUser_Id(12L, 1L)).thenReturn(Optional.of(expired));

        GoneException exception = assertThrows(
            GoneException.class,
            () -> paymentService.createPayment(1L, "idem-1", request)
        );

        assertEquals(ErrorCode.INTENT_EXPIRED, exception.getErrorCode());
    }

    @Test
    void intent_already_converted_returns_conflict() {
        CreatePaymentRequest request = validCreateRequest();
        request.setPaymentIntentId(12L);

        PaymentIntent converted = new PaymentIntent();
        converted.setId(12L);
        converted.setPayeeAccountNumber("ACC-200");
        converted.setStatus(PaymentIntentStatus.CONVERTED);
        converted.setCreatedAt(LocalDateTime.now());

        when(paymentIntentRepository.findByIdAndInitiatedByUser_Id(12L, 1L)).thenReturn(Optional.of(converted));

        ConflictException exception = assertThrows(
            ConflictException.class,
            () -> paymentService.createPayment(1L, "idem-1", request)
        );

        assertEquals(ErrorCode.INTENT_ALREADY_CONVERTED, exception.getErrorCode());
    }

    private CreatePaymentRequest validCreateRequest() {
        CreatePaymentRequest request = new CreatePaymentRequest();
        request.setSourceAccount("ACC-100");
        request.setDestAccount("ACC-200");
        request.setAmount(new BigDecimal("10.25"));
        request.setCurrency("USD");
        request.setReference("test");
        return request;
    }

    private Payment existingPayment() {
        Payment payment = new Payment();
        payment.setId(99L);
        payment.setSourceAccount("ACC-100");
        payment.setDestAccount("ACC-200");
        payment.setAmount(new BigDecimal("10.25"));
        payment.setCurrency("USD");
        payment.setReference("test");
        User user = user(1L);
        payment.setCreatedByUser(user);
        return payment;
    }

    private Payment paymentWithStatus(PaymentStatus status) {
        Payment payment = new Payment();
        payment.setId(1L);
        payment.setSourceAccount("ACC-100");
        payment.setDestAccount("ACC-200");
        payment.setAmount(new BigDecimal("100.00"));
        payment.setCurrency("USD");
        payment.setStatus(status);
        payment.setVersion(0);
        payment.setCreatedByUser(user(1L));
        payment.setCreatedAt(LocalDateTime.now());
        payment.setUpdatedAt(LocalDateTime.now());
        return payment;
    }

    private User user(Long id) {
        User user = new User();
        user.setId(id);
        user.setEmail("u@test.com");
        return user;
    }
}
