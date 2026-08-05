package com.example.payments.dto;

public class PaymentIntentRequest {

    private String payeeAccountNumber;

    public String getPayeeAccountNumber() {
        return payeeAccountNumber;
    }

    public void setPayeeAccountNumber(String payeeAccountNumber) {
        this.payeeAccountNumber = payeeAccountNumber;
    }
}
