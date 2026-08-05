package com.example.payments.service;

import com.example.payments.dto.CreatePaymentRequest;
import com.example.payments.dto.CreatePaymentResult;
import com.example.payments.model.PaymentStatus;
import com.example.payments.model.RoleType;
import com.example.payments.model.entity.Payment;
import com.example.payments.model.entity.PaymentSendAttempt;
import com.example.payments.model.entity.PaymentStatusHistory;
import java.util.List;

public interface PaymentService {

    CreatePaymentResult createPayment(Long userId, String idempotencyKey, CreatePaymentRequest request);

    Payment getPayment(Long userId, Long id);

    List<Payment> listPayments(Long userId, String account, RoleType role, PaymentStatus status);

    List<PaymentStatusHistory> getPaymentHistory(Long userId, Long id);

    Payment transitionStatus(Long userId, Long paymentId, PaymentStatus newStatus, String reason);

    void sendPayment(Long userId, Long paymentId);

    List<PaymentSendAttempt> getSendAttempts(Long userId, Long paymentId);
}
