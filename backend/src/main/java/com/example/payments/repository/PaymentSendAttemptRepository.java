package com.example.payments.repository;

import com.example.payments.model.entity.PaymentSendAttempt;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentSendAttemptRepository extends JpaRepository<PaymentSendAttempt, Long> {

    List<PaymentSendAttempt> findByPaymentIdOrderByAttemptNumberAsc(Long paymentId);
}
