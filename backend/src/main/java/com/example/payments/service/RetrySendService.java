package com.example.payments.service;

import com.example.payments.model.ErrorCode;
import com.example.payments.model.PaymentStatus;
import com.example.payments.model.SendAttemptOutcome;
import com.example.payments.model.entity.Payment;
import com.example.payments.model.entity.PaymentSendAttempt;
import com.example.payments.model.entity.PaymentStatusHistory;
import com.example.payments.repository.PaymentRepository;
import com.example.payments.repository.PaymentSendAttemptRepository;
import com.example.payments.repository.PaymentStatusHistoryRepository;
import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RetrySendService {

    private final PaymentRepository paymentRepository;
    private final PaymentSendAttemptRepository paymentSendAttemptRepository;
    private final PaymentStatusHistoryRepository historyRepository;
    private final int maxRetries;
    private final long backoffBaseMillis;
    private final double sendSuccessRate;

    public RetrySendService(
        PaymentRepository paymentRepository,
        PaymentSendAttemptRepository paymentSendAttemptRepository,
        PaymentStatusHistoryRepository historyRepository,
        @Value("${payment.send.max-retries:3}") int maxRetries,
        @Value("${payment.send.backoff-base-millis:1000}") long backoffBaseMillis,
        @Value("${payment.send.success-rate:0.8}") double sendSuccessRate
    ) {
        this.paymentRepository = paymentRepository;
        this.paymentSendAttemptRepository = paymentSendAttemptRepository;
        this.historyRepository = historyRepository;
        this.maxRetries = maxRetries;
        this.backoffBaseMillis = backoffBaseMillis;
        this.sendSuccessRate = sendSuccessRate;
    }

    @Async
    @Transactional
    public void attemptSend(Long paymentId) {
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            Payment payment = paymentRepository.findById(paymentId).orElse(null);
            if (payment == null || payment.getStatus() == PaymentStatus.COMPLETED || payment.getStatus() == PaymentStatus.FAILED) {
                return;
            }

            SendAttemptOutcome outcome = simulateOutcome();
            logAttempt(payment, attempt, outcome);
            if (outcome == SendAttemptOutcome.SUCCESS) {
                Payment sentState = transitionStatus(payment, PaymentStatus.SENT, "Send attempt succeeded");
                if (sentState != null) {
                    transitionStatus(sentState, PaymentStatus.COMPLETED, "Payment completed after send");
                }
                return;
            }

            sleep(backoffBaseMillis * (long) Math.pow(2, attempt - 1));
        }

        Payment latest = paymentRepository.findById(paymentId).orElse(null);
        if (latest != null && latest.getStatus() != PaymentStatus.COMPLETED && latest.getStatus() != PaymentStatus.FAILED) {
            transitionStatus(latest, PaymentStatus.FAILED, ErrorCode.NETWORK_ERROR.name());
        }
    }

    private SendAttemptOutcome simulateOutcome() {
        double random = ThreadLocalRandom.current().nextDouble();
        if (random <= sendSuccessRate) {
            return SendAttemptOutcome.SUCCESS;
        }
        return random <= (sendSuccessRate + ((1 - sendSuccessRate) / 2))
            ? SendAttemptOutcome.NETWORK_ERROR
            : SendAttemptOutcome.TIMEOUT;
    }

    private void logAttempt(Payment payment, int attemptNumber, SendAttemptOutcome outcome) {
        PaymentSendAttempt attempt = new PaymentSendAttempt();
        attempt.setPayment(payment);
        attempt.setAttemptNumber(attemptNumber);
        attempt.setOutcome(outcome);
        attempt.setAttemptedAt(LocalDateTime.now());
        paymentSendAttemptRepository.save(attempt);
    }

    private Payment transitionStatus(Payment payment, PaymentStatus targetStatus, String reason) {
        PaymentStatus current = payment.getStatus();
        if (current == targetStatus) {
            return payment;
        }

        int rows = paymentRepository.compareAndSwapStatus(
            payment.getId(),
            current,
            payment.getVersion(),
            targetStatus,
            targetStatus == PaymentStatus.FAILED ? reason : null,
            LocalDateTime.now()
        );
        if (rows == 0) {
            return null;
        }

        Payment updated = paymentRepository.findById(payment.getId()).orElse(null);
        if (updated == null) {
            return null;
        }

        PaymentStatusHistory history = new PaymentStatusHistory();
        history.setPayment(updated);
        history.setFromStatus(current);
        history.setToStatus(targetStatus);
        history.setChangedAt(LocalDateTime.now());
        history.setReason(reason);
        historyRepository.save(history);
        return updated;
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}
