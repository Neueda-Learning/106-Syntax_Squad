package com.example.payments.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.payments.model.PaymentStatus;
import com.example.payments.model.SendAttemptOutcome;
import com.example.payments.model.entity.Payment;
import com.example.payments.model.entity.PaymentSendAttempt;
import com.example.payments.model.entity.User;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PaymentSendAttemptRepositoryTest extends AbstractRepositoryTest {

    private PaymentSendAttemptRepository sendAttemptRepository;
    private Payment payment;

    @BeforeEach
    void setUp() {
        sendAttemptRepository = new PaymentSendAttemptRepository(jdbcTemplate);
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
        newPayment.setAmount(new BigDecimal("50.00"));
        newPayment.setCurrency("USD");
        newPayment.setStatus(PaymentStatus.VALIDATED);
        newPayment.setIdempotencyKey("idem-attempt-1");
        newPayment.setCreatedByUser(savedUser);
        newPayment.setVersion(0);
        newPayment.setCreatedAt(LocalDateTime.now());
        newPayment.setUpdatedAt(LocalDateTime.now());
        payment = paymentRepository.save(newPayment);
    }

    private PaymentSendAttempt newAttempt(int attemptNumber, SendAttemptOutcome outcome) {
        PaymentSendAttempt attempt = new PaymentSendAttempt();
        attempt.setPayment(payment);
        attempt.setAttemptNumber(attemptNumber);
        attempt.setOutcome(outcome);
        attempt.setAttemptedAt(LocalDateTime.now());
        return attempt;
    }

    @Test
    void save_insertsNewAttempt_andAssignsId() {
        PaymentSendAttempt saved = sendAttemptRepository.save(newAttempt(1, SendAttemptOutcome.NETWORK_ERROR));

        assertTrue(saved.getId() > 0);
    }

    @Test
    void save_updatesExistingAttempt() {
        PaymentSendAttempt saved = sendAttemptRepository.save(newAttempt(1, SendAttemptOutcome.NETWORK_ERROR));
        saved.setOutcome(SendAttemptOutcome.SUCCESS);

        sendAttemptRepository.save(saved);

        List<PaymentSendAttempt> attempts = sendAttemptRepository.findByPaymentIdOrderByAttemptNumberAsc(payment.getId());
        assertEquals(1, attempts.size());
        assertEquals(SendAttemptOutcome.SUCCESS, attempts.get(0).getOutcome());
    }

    @Test
    void findByPaymentIdOrderByAttemptNumberAsc_returnsOrderedAttempts() {
        sendAttemptRepository.save(newAttempt(2, SendAttemptOutcome.TIMEOUT));
        sendAttemptRepository.save(newAttempt(1, SendAttemptOutcome.NETWORK_ERROR));

        List<PaymentSendAttempt> attempts = sendAttemptRepository.findByPaymentIdOrderByAttemptNumberAsc(payment.getId());

        assertEquals(2, attempts.size());
        assertEquals(1, attempts.get(0).getAttemptNumber());
        assertEquals(2, attempts.get(1).getAttemptNumber());
    }
}
