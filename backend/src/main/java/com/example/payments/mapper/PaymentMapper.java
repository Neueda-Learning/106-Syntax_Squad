package com.example.payments.mapper;

import com.example.payments.dto.SendAttemptResponse;
import com.example.payments.dto.PaymentHistoryResponse;
import com.example.payments.dto.PaymentResponse;
import com.example.payments.model.entity.Payment;
import com.example.payments.model.entity.PaymentSendAttempt;
import com.example.payments.model.entity.PaymentStatusHistory;
import org.springframework.stereotype.Component;

@Component
public class PaymentMapper {

    public PaymentResponse toResponse(Payment payment) {
        PaymentResponse response = new PaymentResponse();
        response.setId(payment.getId());
        response.setSourceAccount(payment.getSourceAccount());
        response.setDestAccount(payment.getDestAccount());
        response.setAmount(payment.getAmount());
        response.setCurrency(payment.getCurrency());
        response.setReference(payment.getReference());
        response.setStatus(payment.getStatus());
        response.setErrorCode(payment.getErrorCode());
        response.setPaymentIntentId(payment.getPaymentIntent() == null ? null : payment.getPaymentIntent().getId());
        response.setCreatedAt(payment.getCreatedAt());
        response.setUpdatedAt(payment.getUpdatedAt());
        return response;
    }

    public PaymentHistoryResponse toHistoryResponse(PaymentStatusHistory history) {
        PaymentHistoryResponse response = new PaymentHistoryResponse();
        response.setId(history.getId());
        response.setFromStatus(history.getFromStatus());
        response.setToStatus(history.getToStatus());
        response.setChangedAt(history.getChangedAt());
        response.setReason(history.getReason());
        return response;
    }

    public SendAttemptResponse toAttemptResponse(PaymentSendAttempt attempt) {
        SendAttemptResponse response = new SendAttemptResponse();
        response.setAttemptNumber(attempt.getAttemptNumber());
        response.setOutcome(attempt.getOutcome());
        response.setAttemptedAt(attempt.getAttemptedAt());
        return response;
    }
}
