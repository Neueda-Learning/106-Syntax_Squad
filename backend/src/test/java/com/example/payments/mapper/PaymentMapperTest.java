package com.example.payments.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.example.payments.dto.PaymentHistoryResponse;
import com.example.payments.dto.PaymentResponse;
import com.example.payments.dto.SendAttemptResponse;
import com.example.payments.model.PaymentStatus;
import com.example.payments.model.SendAttemptOutcome;
import com.example.payments.model.entity.Payment;
import com.example.payments.model.entity.PaymentIntent;
import com.example.payments.model.entity.PaymentSendAttempt;
import com.example.payments.model.entity.PaymentStatusHistory;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PaymentMapperTest {

    private PaymentMapper paymentMapper;

    @BeforeEach
    void setUp() {
        paymentMapper = new PaymentMapper();
    }

    @Test
    void to_response_maps_all_fields_when_payment_contains_intent() {
        PaymentIntent intent = new PaymentIntent();
        intent.setId(77L);

        Payment payment = new Payment();
        payment.setId(10L);
        payment.setSourceAccount("ACC-100");
        payment.setDestAccount("ACC-200");
        payment.setAmount(new BigDecimal("250.50"));
        payment.setCurrency("USD");
        payment.setReference("rent");
        payment.setStatus(PaymentStatus.SENT);
        payment.setErrorCode("NETWORK_ERROR");
        payment.setPaymentIntent(intent);
        payment.setCreatedAt(LocalDateTime.of(2026, 8, 5, 9, 0));
        payment.setUpdatedAt(LocalDateTime.of(2026, 8, 5, 9, 5));

        PaymentResponse response = paymentMapper.toResponse(payment);

        assertEquals(10L, response.getId());
        assertEquals("ACC-100", response.getSourceAccount());
        assertEquals("ACC-200", response.getDestAccount());
        assertEquals(new BigDecimal("250.50"), response.getAmount());
        assertEquals("USD", response.getCurrency());
        assertEquals("rent", response.getReference());
        assertEquals(PaymentStatus.SENT, response.getStatus());
        assertEquals("NETWORK_ERROR", response.getErrorCode());
        assertEquals(77L, response.getPaymentIntentId());
        assertEquals(LocalDateTime.of(2026, 8, 5, 9, 0), response.getCreatedAt());
        assertEquals(LocalDateTime.of(2026, 8, 5, 9, 5), response.getUpdatedAt());
    }

    @Test
    void to_response_sets_payment_intent_id_to_null_when_intent_is_missing() {
        Payment payment = new Payment();
        payment.setId(11L);
        payment.setSourceAccount("ACC-300");
        payment.setDestAccount("ACC-400");
        payment.setAmount(new BigDecimal("15.00"));
        payment.setCurrency("EUR");
        payment.setStatus(PaymentStatus.CREATED);
        payment.setPaymentIntent(null);

        PaymentResponse response = paymentMapper.toResponse(payment);

        assertEquals(11L, response.getId());
        assertNull(response.getPaymentIntentId());
    }

    @Test
    void to_history_response_maps_fields_and_preserves_null_reason() {
        PaymentStatusHistory history = new PaymentStatusHistory();
        history.setId(5L);
        history.setFromStatus(PaymentStatus.CREATED);
        history.setToStatus(PaymentStatus.VALIDATED);
        history.setChangedAt(LocalDateTime.of(2026, 8, 5, 11, 30));
        history.setReason(null);

        PaymentHistoryResponse response = paymentMapper.toHistoryResponse(history);

        assertEquals(5L, response.getId());
        assertEquals(PaymentStatus.CREATED, response.getFromStatus());
        assertEquals(PaymentStatus.VALIDATED, response.getToStatus());
        assertEquals(LocalDateTime.of(2026, 8, 5, 11, 30), response.getChangedAt());
        assertNull(response.getReason());
    }

    @Test
    void to_attempt_response_maps_attempt_number_outcome_and_time() {
        PaymentSendAttempt attempt = new PaymentSendAttempt();
        attempt.setAttemptNumber(3);
        attempt.setOutcome(SendAttemptOutcome.TIMEOUT);
        attempt.setAttemptedAt(LocalDateTime.of(2026, 8, 5, 12, 0));

        SendAttemptResponse response = paymentMapper.toAttemptResponse(attempt);

        assertEquals(3, response.getAttemptNumber());
        assertEquals(SendAttemptOutcome.TIMEOUT, response.getOutcome());
        assertEquals(LocalDateTime.of(2026, 8, 5, 12, 0), response.getAttemptedAt());
    }
}

