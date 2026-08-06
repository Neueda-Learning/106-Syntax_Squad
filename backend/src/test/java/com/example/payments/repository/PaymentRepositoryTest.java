package com.example.payments.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.payments.model.PaymentStatus;
import com.example.payments.model.entity.Payment;
import com.example.payments.model.entity.User;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PaymentRepositoryTest extends AbstractRepositoryTest {

    private PaymentRepository paymentRepository;
    private User payer;

    @BeforeEach
    void setUp() {
        paymentRepository = new PaymentRepository(jdbcTemplate);
        UserRepository userRepository = new UserRepository(jdbcTemplate);

        User user = new User();
        user.setEmail("payer@example.com");
        user.setPasswordHash("hash");
        user.setFullName("Payer");
        user.setCreatedAt(LocalDateTime.now());
        payer = userRepository.save(user);
    }

    private Payment newPayment(String sourceAccount, String destAccount, BigDecimal amount, PaymentStatus status, String idempotencyKey) {
        Payment payment = new Payment();
        payment.setSourceAccount(sourceAccount);
        payment.setDestAccount(destAccount);
        payment.setAmount(amount);
        payment.setCurrency("USD");
        payment.setStatus(status);
        payment.setIdempotencyKey(idempotencyKey);
        payment.setCreatedByUser(payer);
        payment.setVersion(0);
        payment.setCreatedAt(LocalDateTime.now());
        payment.setUpdatedAt(LocalDateTime.now());
        return payment;
    }

    @Test
    void save_insertsNewPayment_andAssignsId() {
        Payment saved = paymentRepository.save(newPayment("ACC-100", "ACC-200", new BigDecimal("10.00"), PaymentStatus.CREATED, "idem-1"));

        assertTrue(saved.getId() > 0);
    }

    @Test
    void save_updatesExistingPayment() {
        Payment saved = paymentRepository.save(newPayment("ACC-100", "ACC-200", new BigDecimal("10.00"), PaymentStatus.CREATED, "idem-2"));
        saved.setStatus(PaymentStatus.VALIDATED);
        saved.setVersion(saved.getVersion() + 1);

        paymentRepository.save(saved);

        Optional<Payment> found = paymentRepository.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals(PaymentStatus.VALIDATED, found.get().getStatus());
    }

    @Test
    void findById_returnsEmpty_whenMissing() {
        assertTrue(paymentRepository.findById(999L).isEmpty());
    }

    @Test
    void findByIdempotencyKey_returnsMatch() {
        paymentRepository.save(newPayment("ACC-100", "ACC-200", new BigDecimal("10.00"), PaymentStatus.CREATED, "idem-3"));

        Optional<Payment> found = paymentRepository.findByIdempotencyKey("idem-3");

        assertTrue(found.isPresent());
    }

    @Test
    void findByCreatedByUser_IdOrderByCreatedAtDesc_returnsPayments() {
        paymentRepository.save(newPayment("ACC-100", "ACC-200", new BigDecimal("10.00"), PaymentStatus.CREATED, "idem-4"));
        paymentRepository.save(newPayment("ACC-100", "ACC-300", new BigDecimal("20.00"), PaymentStatus.FAILED, "idem-5"));

        List<Payment> payments = paymentRepository.findByCreatedByUser_IdOrderByCreatedAtDesc(payer.getId());

        assertEquals(2, payments.size());
    }

    @Test
    void findByCreatedByUser_IdAndStatusOrderByCreatedAtDesc_filtersByStatus() {
        paymentRepository.save(newPayment("ACC-100", "ACC-200", new BigDecimal("10.00"), PaymentStatus.CREATED, "idem-6"));
        paymentRepository.save(newPayment("ACC-100", "ACC-300", new BigDecimal("20.00"), PaymentStatus.FAILED, "idem-7"));

        List<Payment> payments = paymentRepository.findByCreatedByUser_IdAndStatusOrderByCreatedAtDesc(payer.getId(), PaymentStatus.FAILED);

        assertEquals(1, payments.size());
        assertEquals(PaymentStatus.FAILED, payments.get(0).getStatus());
    }

    @Test
    void findBySourceAccountOrderByCreatedAtDesc_returnsPayments() {
        paymentRepository.save(newPayment("ACC-100", "ACC-200", new BigDecimal("10.00"), PaymentStatus.CREATED, "idem-8"));

        List<Payment> payments = paymentRepository.findBySourceAccountOrderByCreatedAtDesc("ACC-100");

        assertEquals(1, payments.size());
    }

    @Test
    void findBySourceAccountInOrderByCreatedAtDesc_returnsPayments_forMultipleAccounts() {
        paymentRepository.save(newPayment("ACC-100", "ACC-200", new BigDecimal("10.00"), PaymentStatus.CREATED, "idem-9"));
        paymentRepository.save(newPayment("ACC-101", "ACC-200", new BigDecimal("15.00"), PaymentStatus.CREATED, "idem-10"));

        List<Payment> payments = paymentRepository.findBySourceAccountInOrderByCreatedAtDesc(List.of("ACC-100", "ACC-101"));

        assertEquals(2, payments.size());
    }

    @Test
    void findBySourceAccountInAndStatusOrderByCreatedAtDesc_filtersByStatus() {
        paymentRepository.save(newPayment("ACC-100", "ACC-200", new BigDecimal("10.00"), PaymentStatus.CREATED, "idem-11"));
        paymentRepository.save(newPayment("ACC-100", "ACC-200", new BigDecimal("15.00"), PaymentStatus.COMPLETED, "idem-12"));

        List<Payment> payments = paymentRepository
            .findBySourceAccountInAndStatusOrderByCreatedAtDesc(List.of("ACC-100"), PaymentStatus.COMPLETED);

        assertEquals(1, payments.size());
        assertEquals(PaymentStatus.COMPLETED, payments.get(0).getStatus());
    }

    @Test
    void findByDestAccountOrderByCreatedAtDesc_returnsPayments() {
        paymentRepository.save(newPayment("ACC-100", "ACC-200", new BigDecimal("10.00"), PaymentStatus.CREATED, "idem-13"));

        List<Payment> payments = paymentRepository.findByDestAccountOrderByCreatedAtDesc("ACC-200");

        assertEquals(1, payments.size());
    }

    @Test
    void findByDestAccountInOrderByCreatedAtDesc_returnsPayments() {
        paymentRepository.save(newPayment("ACC-100", "ACC-200", new BigDecimal("10.00"), PaymentStatus.CREATED, "idem-14"));
        paymentRepository.save(newPayment("ACC-100", "ACC-201", new BigDecimal("10.00"), PaymentStatus.CREATED, "idem-15"));

        List<Payment> payments = paymentRepository.findByDestAccountInOrderByCreatedAtDesc(List.of("ACC-200", "ACC-201"));

        assertEquals(2, payments.size());
    }

    @Test
    void findByDestAccountAndStatusOrderByCreatedAtDesc_filtersByStatus() {
        paymentRepository.save(newPayment("ACC-100", "ACC-200", new BigDecimal("10.00"), PaymentStatus.CREATED, "idem-16"));
        paymentRepository.save(newPayment("ACC-100", "ACC-200", new BigDecimal("10.00"), PaymentStatus.COMPLETED, "idem-17"));

        List<Payment> payments = paymentRepository.findByDestAccountAndStatusOrderByCreatedAtDesc("ACC-200", PaymentStatus.COMPLETED);

        assertEquals(1, payments.size());
    }

    @Test
    void findByDestAccountInAndStatusOrderByCreatedAtDesc_filtersByStatus() {
        paymentRepository.save(newPayment("ACC-100", "ACC-200", new BigDecimal("10.00"), PaymentStatus.CREATED, "idem-18"));
        paymentRepository.save(newPayment("ACC-100", "ACC-201", new BigDecimal("10.00"), PaymentStatus.COMPLETED, "idem-19"));

        List<Payment> payments = paymentRepository
            .findByDestAccountInAndStatusOrderByCreatedAtDesc(List.of("ACC-200", "ACC-201"), PaymentStatus.COMPLETED);

        assertEquals(1, payments.size());
        assertEquals("ACC-201", payments.get(0).getDestAccount());
    }

    @Test
    void findByAccountList_returnsEmptyList_whenAccountsEmpty() {
        List<Payment> payments = paymentRepository.findBySourceAccountInOrderByCreatedAtDesc(List.of());

        assertEquals(0, payments.size());
    }

    @Test
    void existsByIdAndSourceAccountOrIdAndDestAccount_matchesEitherSide() {
        Payment saved = paymentRepository.save(newPayment("ACC-100", "ACC-200", new BigDecimal("10.00"), PaymentStatus.CREATED, "idem-20"));

        boolean matchesSource = paymentRepository
            .existsByIdAndSourceAccountOrIdAndDestAccount(saved.getId(), "ACC-100", -1L, "NOPE");
        boolean matchesDest = paymentRepository
            .existsByIdAndSourceAccountOrIdAndDestAccount(-1L, "NOPE", saved.getId(), "ACC-200");
        boolean matchesNeither = paymentRepository
            .existsByIdAndSourceAccountOrIdAndDestAccount(-1L, "NOPE", -1L, "NOPE");

        assertTrue(matchesSource);
        assertTrue(matchesDest);
        assertTrue(!matchesNeither);
    }

    @Test
    void compareAndSwapStatus_updatesStatus_whenExpectedStateMatches() {
        Payment saved = paymentRepository.save(newPayment("ACC-100", "ACC-200", new BigDecimal("10.00"), PaymentStatus.CREATED, "idem-21"));

        int rows = paymentRepository.compareAndSwapStatus(
            saved.getId(),
            PaymentStatus.CREATED,
            saved.getVersion(),
            PaymentStatus.VALIDATED,
            null,
            LocalDateTime.now()
        );

        assertEquals(1, rows);
        Optional<Payment> updated = paymentRepository.findById(saved.getId());
        assertTrue(updated.isPresent());
        assertEquals(PaymentStatus.VALIDATED, updated.get().getStatus());
        assertEquals(saved.getVersion() + 1, updated.get().getVersion());
    }

    @Test
    void compareAndSwapStatus_returnsZeroRows_whenVersionMismatch() {
        Payment saved = paymentRepository.save(newPayment("ACC-100", "ACC-200", new BigDecimal("10.00"), PaymentStatus.CREATED, "idem-22"));

        int rows = paymentRepository.compareAndSwapStatus(
            saved.getId(),
            PaymentStatus.CREATED,
            saved.getVersion() + 1,
            PaymentStatus.VALIDATED,
            null,
            LocalDateTime.now()
        );

        assertEquals(0, rows);
        Optional<Payment> unchanged = paymentRepository.findById(saved.getId());
        assertTrue(unchanged.isPresent());
        assertEquals(PaymentStatus.CREATED, unchanged.get().getStatus());
    }

    @Test
    void compareAndSwapStatus_returnsZeroRows_whenExpectedStatusMismatch() {
        Payment saved = paymentRepository.save(newPayment("ACC-100", "ACC-200", new BigDecimal("10.00"), PaymentStatus.CREATED, "idem-23"));

        int rows = paymentRepository.compareAndSwapStatus(
            saved.getId(),
            PaymentStatus.VALIDATED,
            saved.getVersion(),
            PaymentStatus.SENT,
            null,
            LocalDateTime.now()
        );

        assertEquals(0, rows);
    }

    @Test
    void compareAndSwapStatus_setsErrorCode_whenTransitioningToFailed() {
        Payment saved = paymentRepository.save(newPayment("ACC-100", "ACC-200", new BigDecimal("10.00"), PaymentStatus.CREATED, "idem-24"));

        paymentRepository.compareAndSwapStatus(
            saved.getId(),
            PaymentStatus.CREATED,
            saved.getVersion(),
            PaymentStatus.FAILED,
            "INSUFFICIENT_FUNDS",
            LocalDateTime.now()
        );

        Optional<Payment> updated = paymentRepository.findById(saved.getId());
        assertTrue(updated.isPresent());
        assertEquals(PaymentStatus.FAILED, updated.get().getStatus());
        assertEquals("INSUFFICIENT_FUNDS", updated.get().getErrorCode());
    }
}
