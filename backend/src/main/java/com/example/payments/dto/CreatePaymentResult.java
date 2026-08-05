package com.example.payments.dto;

import com.example.payments.model.entity.Payment;

public class CreatePaymentResult {

    private final Payment payment;
    private final boolean created;

    public CreatePaymentResult(Payment payment, boolean created) {
        this.payment = payment;
        this.created = created;
    }

    public Payment getPayment() {
        return payment;
    }

    public boolean isCreated() {
        return created;
    }
}
