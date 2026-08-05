package com.example.payments.controller;

import com.example.payments.dto.PaymentIntentRequest;
import com.example.payments.dto.PaymentIntentResponse;
import com.example.payments.model.entity.PaymentIntent;
import com.example.payments.security.CurrentUserService;
import com.example.payments.service.PaymentIntentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payment-intents")
public class PaymentIntentController {

    private final PaymentIntentService paymentIntentService;
    private final CurrentUserService currentUserService;

    public PaymentIntentController(PaymentIntentService paymentIntentService, CurrentUserService currentUserService) {
        this.paymentIntentService = paymentIntentService;
        this.currentUserService = currentUserService;
    }

    @PostMapping
    public ResponseEntity<PaymentIntentResponse> createIntent(@RequestBody PaymentIntentRequest request) {
        Long userId = currentUserService.getCurrentUserId();
        PaymentIntent intent = paymentIntentService.createIntent(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(userId, intent));
    }

    @GetMapping("/{id}")
    public PaymentIntentResponse getIntent(@PathVariable Long id) {
        Long userId = currentUserService.getCurrentUserId();
        return toResponse(userId, paymentIntentService.getIntent(userId, id));
    }

    private PaymentIntentResponse toResponse(Long userId, PaymentIntent intent) {
        PaymentIntentResponse response = new PaymentIntentResponse();
        response.setIntentId(intent.getId());
        response.setIdempotencyKey(intent.getIdempotencyKey());
        response.setPayeeAccountNumber(intent.getPayeeAccountNumber());
        response.setPayeeNickname(paymentIntentService.resolveNickname(userId, intent.getPayeeAccountNumber()));
        response.setStatus(intent.getStatus());
        response.setCreatedAt(intent.getCreatedAt());
        return response;
    }
}
