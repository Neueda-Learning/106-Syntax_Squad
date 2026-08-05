package com.example.payments.dto;

import com.example.payments.model.PaymentIntentStatus;
import java.time.LocalDateTime;

public class PaymentIntentResponse {

    private Long intentId;
    private String idempotencyKey;
    private String payeeAccountNumber;
    private String payeeNickname;
    private PaymentIntentStatus status;
    private LocalDateTime createdAt;

    public Long getIntentId() {
        return intentId;
    }

    public void setIntentId(Long intentId) {
        this.intentId = intentId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public String getPayeeAccountNumber() {
        return payeeAccountNumber;
    }

    public void setPayeeAccountNumber(String payeeAccountNumber) {
        this.payeeAccountNumber = payeeAccountNumber;
    }

    public String getPayeeNickname() {
        return payeeNickname;
    }

    public void setPayeeNickname(String payeeNickname) {
        this.payeeNickname = payeeNickname;
    }

    public PaymentIntentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentIntentStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
