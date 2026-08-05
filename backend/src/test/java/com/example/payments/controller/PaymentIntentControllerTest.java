package com.example.payments.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.payments.dto.PaymentIntentRequest;
import com.example.payments.dto.PaymentIntentResponse;
import com.example.payments.model.PaymentIntentStatus;
import com.example.payments.model.entity.PaymentIntent;
import com.example.payments.security.CurrentUserService;
import com.example.payments.service.PaymentIntentService;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class PaymentIntentControllerTest {

    @Mock
    private PaymentIntentService paymentIntentService;

    @Mock
    private CurrentUserService currentUserService;

    private PaymentIntentController paymentIntentController;

    @BeforeEach
    void setUp() {
        paymentIntentController = new PaymentIntentController(paymentIntentService, currentUserService);
    }

    @Test
    void create_intent_returns_created_response_with_resolved_nickname() {
        PaymentIntentRequest request = new PaymentIntentRequest();
        request.setPayeeAccountNumber("ACC-500");

        PaymentIntent intent = new PaymentIntent();
        intent.setId(40L);
        intent.setIdempotencyKey("idem-40");
        intent.setPayeeAccountNumber("ACC-500");
        intent.setStatus(PaymentIntentStatus.CREATED);
        intent.setCreatedAt(LocalDateTime.of(2026, 8, 5, 12, 30));

        when(currentUserService.getCurrentUserId()).thenReturn(2L);
        when(paymentIntentService.createIntent(2L, request)).thenReturn(intent);
        when(paymentIntentService.resolveNickname(2L, "ACC-500")).thenReturn("Landlord");

        ResponseEntity<PaymentIntentResponse> response = paymentIntentController.createIntent(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(40L, response.getBody().getIntentId());
        assertEquals("idem-40", response.getBody().getIdempotencyKey());
        assertEquals("ACC-500", response.getBody().getPayeeAccountNumber());
        assertEquals("Landlord", response.getBody().getPayeeNickname());
        assertEquals(PaymentIntentStatus.CREATED, response.getBody().getStatus());
        assertEquals(LocalDateTime.of(2026, 8, 5, 12, 30), response.getBody().getCreatedAt());
        verify(paymentIntentService).createIntent(2L, request);
        verify(paymentIntentService).resolveNickname(2L, "ACC-500");
    }

    @Test
    void get_intent_returns_response_with_null_nickname_when_payee_not_saved() {
        PaymentIntent intent = new PaymentIntent();
        intent.setId(41L);
        intent.setIdempotencyKey("idem-41");
        intent.setPayeeAccountNumber("ACC-600");
        intent.setStatus(PaymentIntentStatus.CREATED);
        intent.setCreatedAt(LocalDateTime.of(2026, 8, 5, 13, 0));

        when(currentUserService.getCurrentUserId()).thenReturn(6L);
        when(paymentIntentService.getIntent(6L, 41L)).thenReturn(intent);
        when(paymentIntentService.resolveNickname(6L, "ACC-600")).thenReturn(null);

        PaymentIntentResponse response = paymentIntentController.getIntent(41L);

        assertEquals(41L, response.getIntentId());
        assertEquals("idem-41", response.getIdempotencyKey());
        assertEquals("ACC-600", response.getPayeeAccountNumber());
        assertNull(response.getPayeeNickname());
        assertEquals(PaymentIntentStatus.CREATED, response.getStatus());
        verify(paymentIntentService).getIntent(6L, 41L);
        verify(paymentIntentService).resolveNickname(6L, "ACC-600");
    }
}

