package com.example.payments.dto;

import com.example.payments.model.SendAttemptOutcome;
import java.time.LocalDateTime;

public class SendAttemptResponse {

    private Integer attemptNumber;
    private SendAttemptOutcome outcome;
    private LocalDateTime attemptedAt;

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
