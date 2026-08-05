package com.example.payments.model.entity;

import com.example.payments.model.SendAttemptOutcome;
import java.time.LocalDateTime;

public class PaymentSendAttempt {

    private Long id;

    private Payment payment;

    private Integer attemptNumber;

    private SendAttemptOutcome outcome;

    private LocalDateTime attemptedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Payment getPayment() {
        return payment;
    }

    public void setPayment(Payment payment) {
        this.payment = payment;
    }

    public Integer getAttemptNumber() {
        return attemptNumber;
    }

    public void setAttemptNumber(Integer attemptNumber) {
        this.attemptNumber = attemptNumber;
    }

    public SendAttemptOutcome getOutcome() {
        return outcome;
    }

    public void setOutcome(SendAttemptOutcome outcome) {
        this.outcome = outcome;
    }

    public LocalDateTime getAttemptedAt() {
        return attemptedAt;
    }

    public void setAttemptedAt(LocalDateTime attemptedAt) {
        this.attemptedAt = attemptedAt;
    }
}
