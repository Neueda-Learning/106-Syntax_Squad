package com.example.payments.dto;

import com.example.payments.model.PaymentStatus;

public class UpdateStatusRequest {

    private PaymentStatus newStatus;
    private String reason;

    public PaymentStatus getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(PaymentStatus newStatus) {
        this.newStatus = newStatus;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
