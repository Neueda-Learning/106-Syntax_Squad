package com.example.payments.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.payments.model.PaymentStatus;
import com.example.payments.model.entity.Payment;
import com.example.payments.model.entity.PaymentStatusHistory;
import com.example.payments.model.entity.User;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PaymentStatusHistoryRepositoryTest extends AbstractRepositoryTest {

    private PaymentStatusHistoryRepository historyRepository;
    private Payment payment;

    @BeforeEach
    void setUp() {
        historyRepository = new PaymentStatusHistoryRepository(jdbcTemplate);
        UserRepository userRepository = new UserRepository(jdbcTemplate);
        PaymentRepository paymentRepository = new PaymentRepository(jdbcTemplate);

        User user = new User();
        user.setEmail("payer@example.com");
        user.setPasswordHash("hash");
        user.setFullName("Payer");
        user.setCreatedAt(LocalDateTime.now());
        User savedUser = userRepository.save(user);

        Payment newPayment = new Payment();
        newPayment.setSourceAccount("ACC-100");
        newPayment.setDestAccount("ACC-200");
        newPayment.setAmount(new BigDecimal("75.00"));
        newPayment.setCurrency("USD");
        newPayment.setStatus(PaymentStatus.CREATED);
        newPayment.setIdempotencyKey("idem-history-1");
        newPayment.setCreatedByUser(savedUser);
        newPayment.setVersion(0);
        newPayment.setCreatedAt(LocalDateTime.now());
        newPayment.setUpdatedAt(LocalDateTime.now());
        payment = paymentRepository.save(newPayment);
    }

    private PaymentStatusHistory newHistory(PaymentStatus from, PaymentStatus to, String reason) {
        PaymentStatusHistory history = new PaymentStatusHistory();
        history.setPayment(payment);
        history.setFromStatus(from);
        history.setToStatus(to);
        history.setChangedAt(LocalDateTime.now());
        history.setReason(reason);
        return history;
    }

    @Test
    void save_insertsNewHistory_andAssignsId() {
        PaymentStatusHistory saved = historyRepository.save(newHistory(null, PaymentStatus.CREATED, "Payment created"));

        assertTrue(saved.getId() > 0);
    }

    @Test
    void save_allowsNullFromStatus() {
        PaymentStatusHistory saved = historyRepository.save(newHistory(null, PaymentStatus.CREATED, "Payment created"));

        List<PaymentStatusHistory> history = historyRepository.findByPaymentIdOrderByChangedAtAsc(payment.getId());
        assertEquals(1, history.size());
        assertNull(history.get(0).getFromStatus());
    }

    @Test
    void save_updatesExistingHistory() {
        PaymentStatusHistory saved = historyRepository.save(newHistory(null, PaymentStatus.CREATED, "Payment created"));
        saved.setReason("Updated reason");

        historyRepository.save(saved);

        List<PaymentStatusHistory> history = historyRepository.findByPaymentIdOrderByChangedAtAsc(payment.getId());
        assertEquals("Updated reason", history.get(0).getReason());
    }

    @Test
    void findByPaymentIdOrderByChangedAtAsc_returnsChronologicalOrder() throws InterruptedException {
        historyRepository.save(newHistory(null, PaymentStatus.CREATED, "Created"));
        Thread.sleep(5);
        historyRepository.save(newHistory(PaymentStatus.CREATED, PaymentStatus.VALIDATED, "Validated"));

        List<PaymentStatusHistory> history = historyRepository.findByPaymentIdOrderByChangedAtAsc(payment.getId());

        assertEquals(2, history.size());
        assertEquals(PaymentStatus.CREATED, history.get(0).getToStatus());
        assertEquals(PaymentStatus.VALIDATED, history.get(1).getToStatus());
    }
}
