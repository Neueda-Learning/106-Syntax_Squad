package com.example.payments.repository;

import com.example.payments.model.entity.PaymentIntent;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentIntentRepository extends JpaRepository<PaymentIntent, Long> {

    Optional<PaymentIntent> findByIdAndInitiatedByUser_Id(Long id, Long initiatedByUserId);
}
