package com.example.payments.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.payments.model.PaymentIntentStatus;
import com.example.payments.model.entity.PaymentIntent;
import com.example.payments.model.entity.User;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PaymentIntentRepositoryTest extends AbstractRepositoryTest {

    private PaymentIntentRepository paymentIntentRepository;
    private User initiator;

    @BeforeEach
    void setUp() {
        paymentIntentRepository = new PaymentIntentRepository(jdbcTemplate);
        UserRepository userRepository = new UserRepository(jdbcTemplate);

        User user = new User();
        user.setEmail("initiator@example.com");
        user.setPasswordHash("hash");
        user.setFullName("Initiator");
        user.setCreatedAt(LocalDateTime.now());
        initiator = userRepository.save(user);
    }

    private PaymentIntent newIntent(String payeeAccountNumber) {
        PaymentIntent intent = new PaymentIntent();
        intent.setIdempotencyKey(UUID.randomUUID().toString());
        intent.setInitiatedByUser(initiator);
        intent.setPayeeAccountNumber(payeeAccountNumber);
        intent.setStatus(PaymentIntentStatus.CREATED);
        intent.setCreatedAt(LocalDateTime.now());
        return intent;
    }

    @Test
    void save_insertsNewIntent_andAssignsId() {
        PaymentIntent saved = paymentIntentRepository.save(newIntent("ACC-100"));

        assertTrue(saved.getId() > 0);
    }

    @Test
    void save_updatesExistingIntent() {
        PaymentIntent saved = paymentIntentRepository.save(newIntent("ACC-100"));
        saved.setStatus(PaymentIntentStatus.CONVERTED);

        paymentIntentRepository.save(saved);

        Optional<PaymentIntent> found = paymentIntentRepository.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals(PaymentIntentStatus.CONVERTED, found.get().getStatus());
    }

    @Test
    void findByIdAndInitiatedByUser_Id_returnsMatch() {
        PaymentIntent saved = paymentIntentRepository.save(newIntent("ACC-200"));

        Optional<PaymentIntent> found = paymentIntentRepository.findByIdAndInitiatedByUser_Id(saved.getId(), initiator.getId());

        assertTrue(found.isPresent());
        assertEquals("ACC-200", found.get().getPayeeAccountNumber());
    }

    @Test
    void findByIdAndInitiatedByUser_Id_returnsEmpty_forDifferentUser() {
        PaymentIntent saved = paymentIntentRepository.save(newIntent("ACC-200"));

        Optional<PaymentIntent> found = paymentIntentRepository.findByIdAndInitiatedByUser_Id(saved.getId(), 999L);

        assertTrue(found.isEmpty());
    }

    @Test
    void findById_returnsEmpty_whenMissing() {
        assertTrue(paymentIntentRepository.findById(999L).isEmpty());
    }
}
