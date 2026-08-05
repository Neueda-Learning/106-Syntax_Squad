package com.example.payments.repository;

import com.example.payments.model.entity.PaymentStatusHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentStatusHistoryRepository extends JpaRepository<PaymentStatusHistory, Long> {

    List<PaymentStatusHistory> findByPaymentIdOrderByChangedAtAsc(Long paymentId);
}
