package com.example.payments.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.payments.dto.CreatePaymentRequest;
import com.example.payments.dto.CreatePaymentResult;
import com.example.payments.dto.PaymentHistoryResponse;
import com.example.payments.dto.PaymentResponse;
import com.example.payments.dto.SendAttemptResponse;
import com.example.payments.dto.UpdateStatusRequest;
import com.example.payments.exception.ValidationException;
import com.example.payments.mapper.PaymentMapper;
import com.example.payments.model.ErrorCode;
import com.example.payments.model.PaymentStatus;
import com.example.payments.model.RoleType;
import com.example.payments.model.SendAttemptOutcome;
import com.example.payments.model.entity.Payment;
import com.example.payments.model.entity.PaymentSendAttempt;
import com.example.payments.model.entity.PaymentStatusHistory;
import com.example.payments.security.CurrentUserService;
import com.example.payments.service.PaymentService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    @Mock
    private PaymentService paymentService;

    @Mock
    private PaymentMapper paymentMapper;

    @Mock
    private CurrentUserService currentUserService;

    private PaymentController paymentController;

    @BeforeEach
    void setUp() {
        paymentController = new PaymentController(paymentService, paymentMapper, currentUserService);
    }

    @Test
    void create_payment_returns_created_when_service_creates_new_payment() {
        Payment payment = new Payment();
        payment.setId(100L);
        CreatePaymentRequest request = new CreatePaymentRequest();

        PaymentResponse mapped = new PaymentResponse();
        mapped.setId(100L);

        when(currentUserService.getCurrentUserId()).thenReturn(12L);
        when(paymentService.createPayment(12L, "idem-1", request)).thenReturn(new CreatePaymentResult(payment, true));
        when(paymentMapper.toResponse(payment)).thenReturn(mapped);

        ResponseEntity<PaymentResponse> response = paymentController.createPayment("idem-1", request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(100L, response.getBody().getId());
        verify(paymentService).createPayment(12L, "idem-1", request);
    }

    @Test
    void create_payment_returns_ok_when_idempotent_result_is_reused() {
        Payment payment = new Payment();
        payment.setId(101L);
        CreatePaymentRequest request = new CreatePaymentRequest();

        PaymentResponse mapped = new PaymentResponse();
        mapped.setId(101L);

        when(currentUserService.getCurrentUserId()).thenReturn(12L);
        when(paymentService.createPayment(12L, "idem-2", request)).thenReturn(new CreatePaymentResult(payment, false));
        when(paymentMapper.toResponse(payment)).thenReturn(mapped);

        ResponseEntity<PaymentResponse> response = paymentController.createPayment("idem-2", request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(101L, response.getBody().getId());
    }

    @Test
    void get_payment_returns_mapped_response_for_current_user() {
        Payment payment = new Payment();
        payment.setId(8L);

        PaymentResponse mapped = new PaymentResponse();
        mapped.setId(8L);

        when(currentUserService.getCurrentUserId()).thenReturn(9L);
        when(paymentService.getPayment(9L, 8L)).thenReturn(payment);
        when(paymentMapper.toResponse(payment)).thenReturn(mapped);

        PaymentResponse result = paymentController.getPayment(8L);

        assertEquals(8L, result.getId());
        verify(paymentService).getPayment(9L, 8L);
    }

    @Test
    void list_payments_parses_sent_role_and_returns_mapped_results() {
        Payment payment = new Payment();
        payment.setId(20L);

        PaymentResponse mapped = new PaymentResponse();
        mapped.setId(20L);

        when(currentUserService.getCurrentUserId()).thenReturn(77L);
        when(paymentService.listPayments(77L, "ACC-123", RoleType.SENT, PaymentStatus.CREATED)).thenReturn(List.of(payment));
        when(paymentMapper.toResponse(payment)).thenReturn(mapped);

        List<PaymentResponse> result = paymentController.listPayments("ACC-123", " sent ", PaymentStatus.CREATED);

        assertEquals(1, result.size());
        assertEquals(20L, result.get(0).getId());
        verify(paymentService).listPayments(77L, "ACC-123", RoleType.SENT, PaymentStatus.CREATED);
    }

    @Test
    void list_payments_rejects_blank_role() {
        ValidationException exception = assertThrows(
            ValidationException.class,
            () -> paymentController.listPayments("ACC-123", "   ", null)
        );

        assertEquals(ErrorCode.VALIDATION_FAILED, exception.getErrorCode());
        assertTrue(exception.getMessage().contains("role is required"));
    }

    @Test
    void list_payments_rejects_unknown_role() {
        ValidationException exception = assertThrows(
            ValidationException.class,
            () -> paymentController.listPayments("ACC-123", "owner", null)
        );

        assertEquals(ErrorCode.VALIDATION_FAILED, exception.getErrorCode());
        assertTrue(exception.getMessage().contains("role must be sent or received"));
    }

    @Test
    void get_payment_history_returns_mapped_history_entries() {
        PaymentStatusHistory history = new PaymentStatusHistory();
        history.setId(31L);

        PaymentHistoryResponse mapped = new PaymentHistoryResponse();
        mapped.setId(31L);

        when(currentUserService.getCurrentUserId()).thenReturn(5L);
        when(paymentService.getPaymentHistory(5L, 31L)).thenReturn(List.of(history));
        when(paymentMapper.toHistoryResponse(history)).thenReturn(mapped);

        List<PaymentHistoryResponse> result = paymentController.getPaymentHistory(31L);

        assertEquals(1, result.size());
        assertEquals(31L, result.get(0).getId());
    }

    @Test
    void get_send_attempts_returns_mapped_attempt_entries() {
        PaymentSendAttempt attempt = new PaymentSendAttempt();
        attempt.setAttemptNumber(2);

        SendAttemptResponse mapped = new SendAttemptResponse();
        mapped.setAttemptNumber(2);
        mapped.setOutcome(SendAttemptOutcome.SUCCESS);

        when(currentUserService.getCurrentUserId()).thenReturn(5L);
        when(paymentService.getSendAttempts(5L, 44L)).thenReturn(List.of(attempt));
        when(paymentMapper.toAttemptResponse(attempt)).thenReturn(mapped);

        List<SendAttemptResponse> result = paymentController.getSendAttempts(44L);

        assertEquals(1, result.size());
        assertEquals(2, result.get(0).getAttemptNumber());
        assertEquals(SendAttemptOutcome.SUCCESS, result.get(0).getOutcome());
    }

    @Test
    void transition_status_rejects_null_request() {
        ValidationException exception = assertThrows(
            ValidationException.class,
            () -> paymentController.transitionStatus(10L, null)
        );

        assertEquals(ErrorCode.VALIDATION_FAILED, exception.getErrorCode());
        assertEquals("newStatus is required", exception.getMessage());
    }

    @Test
    void transition_status_rejects_missing_new_status() {
        UpdateStatusRequest request = new UpdateStatusRequest();
        request.setReason("manual");

        ValidationException exception = assertThrows(
            ValidationException.class,
            () -> paymentController.transitionStatus(10L, request)
        );

        assertEquals(ErrorCode.VALIDATION_FAILED, exception.getErrorCode());
        assertEquals("newStatus is required", exception.getMessage());
    }

    @Test
    void transition_status_returns_mapped_response_when_request_is_valid() {
        Payment transitioned = new Payment();
        transitioned.setId(10L);
        transitioned.setStatus(PaymentStatus.SENT);
        transitioned.setUpdatedAt(LocalDateTime.of(2026, 8, 5, 14, 0));

        PaymentResponse mapped = new PaymentResponse();
        mapped.setId(10L);
        mapped.setStatus(PaymentStatus.SENT);

        UpdateStatusRequest request = new UpdateStatusRequest();
        request.setNewStatus(PaymentStatus.SENT);
        request.setReason("manual send");

        when(currentUserService.getCurrentUserId()).thenReturn(1L);
        when(paymentService.transitionStatus(1L, 10L, PaymentStatus.SENT, "manual send")).thenReturn(transitioned);
        when(paymentMapper.toResponse(transitioned)).thenReturn(mapped);

        PaymentResponse result = paymentController.transitionStatus(10L, request);

        assertEquals(10L, result.getId());
        assertEquals(PaymentStatus.SENT, result.getStatus());
    }

    @Test
    void send_payment_returns_accepted_and_calls_service() {
        when(currentUserService.getCurrentUserId()).thenReturn(21L);

        ResponseEntity<Void> response = paymentController.sendPayment(90L);

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        verify(paymentService).sendPayment(21L, 90L);
    }
}

