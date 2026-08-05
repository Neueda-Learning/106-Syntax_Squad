package com.example.payments.repository;

import com.example.payments.model.PaymentStatus;
import com.example.payments.model.entity.Payment;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findBySourceAccountOrderByCreatedAtDesc(String sourceAccount);

    List<Payment> findByCreatedByUser_IdOrderByCreatedAtDesc(Long createdByUserId);

    List<Payment> findBySourceAccountInOrderByCreatedAtDesc(List<String> sourceAccounts);

    List<Payment> findBySourceAccountAndStatusOrderByCreatedAtDesc(String sourceAccount, PaymentStatus status);

    List<Payment> findByCreatedByUser_IdAndStatusOrderByCreatedAtDesc(Long createdByUserId, PaymentStatus status);

    List<Payment> findBySourceAccountInAndStatusOrderByCreatedAtDesc(List<String> sourceAccounts, PaymentStatus status);

    List<Payment> findByDestAccountOrderByCreatedAtDesc(String destAccount);

    List<Payment> findByDestAccountInOrderByCreatedAtDesc(List<String> destAccounts);

    List<Payment> findByDestAccountAndStatusOrderByCreatedAtDesc(String destAccount, PaymentStatus status);

    List<Payment> findByDestAccountInAndStatusOrderByCreatedAtDesc(List<String> destAccounts, PaymentStatus status);

    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    boolean existsByIdAndSourceAccountOrIdAndDestAccount(Long id1, String sourceAccount, Long id2, String destAccount);

    @Modifying
    @Query(
        """
        update Payment p
        set p.status = :newStatus,
            p.errorCode = :errorCode,
            p.updatedAt = :updatedAt,
            p.version = p.version + 1
        where p.id = :paymentId
          and p.status = :expectedStatus
          and p.version = :expectedVersion
        """
    )
    int compareAndSwapStatus(
        @Param("paymentId") Long paymentId,
        @Param("expectedStatus") PaymentStatus expectedStatus,
        @Param("expectedVersion") Integer expectedVersion,
        @Param("newStatus") PaymentStatus newStatus,
        @Param("errorCode") String errorCode,
        @Param("updatedAt") LocalDateTime updatedAt
    );
}
