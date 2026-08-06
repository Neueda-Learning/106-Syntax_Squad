package com.example.payments.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.payments.model.entity.Payee;
import com.example.payments.model.entity.User;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PayeeRepositoryTest extends AbstractRepositoryTest {

    private PayeeRepository payeeRepository;
    private User owner;

    @BeforeEach
    void setUp() {
        payeeRepository = new PayeeRepository(jdbcTemplate);
        UserRepository userRepository = new UserRepository(jdbcTemplate);

        User user = new User();
        user.setEmail("owner@example.com");
        user.setPasswordHash("hash");
        user.setFullName("Owner");
        user.setCreatedAt(LocalDateTime.now());
        owner = userRepository.save(user);
    }

    private Payee newPayee(String accountNumber, String nickname) {
        Payee payee = new Payee();
        payee.setOwnerUser(owner);
        payee.setPayeeAccountNumber(accountNumber);
        payee.setNickname(nickname);
        payee.setCreatedAt(LocalDateTime.now());
        return payee;
    }

    @Test
    void save_insertsNewPayee_andAssignsId() {
        Payee saved = payeeRepository.save(newPayee("ACC-100", "Landlord"));

        assertTrue(saved.getId() > 0);
    }

    @Test
    void save_updatesExistingPayee() {
        Payee saved = payeeRepository.save(newPayee("ACC-100", "Landlord"));
        saved.setNickname("New Landlord");

        payeeRepository.save(saved);

        Optional<Payee> found = payeeRepository.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals("New Landlord", found.get().getNickname());
    }

    @Test
    void findByOwnerUser_IdOrderByCreatedAtDesc_returnsPayees() {
        payeeRepository.save(newPayee("ACC-100", "First"));
        payeeRepository.save(newPayee("ACC-200", "Second"));

        List<Payee> payees = payeeRepository.findByOwnerUser_IdOrderByCreatedAtDesc(owner.getId());

        assertEquals(2, payees.size());
    }

    @Test
    void findByOwnerUser_IdAndPayeeAccountNumber_returnsMatch() {
        payeeRepository.save(newPayee("ACC-300", "Utility Co"));

        Optional<Payee> found = payeeRepository.findByOwnerUser_IdAndPayeeAccountNumber(owner.getId(), "ACC-300");

        assertTrue(found.isPresent());
        assertEquals("Utility Co", found.get().getNickname());
    }

    @Test
    void findByIdAndOwnerUser_Id_returnsMatch() {
        Payee saved = payeeRepository.save(newPayee("ACC-400", "Gym"));

        Optional<Payee> found = payeeRepository.findByIdAndOwnerUser_Id(saved.getId(), owner.getId());

        assertTrue(found.isPresent());
    }

    @Test
    void findByIdAndOwnerUser_Id_returnsEmpty_forDifferentOwner() {
        Payee saved = payeeRepository.save(newPayee("ACC-400", "Gym"));

        Optional<Payee> found = payeeRepository.findByIdAndOwnerUser_Id(saved.getId(), 999L);

        assertTrue(found.isEmpty());
    }

    @Test
    void delete_removesPayee() {
        Payee saved = payeeRepository.save(newPayee("ACC-500", "Gone Soon"));

        payeeRepository.delete(saved);

        assertTrue(payeeRepository.findById(saved.getId()).isEmpty());
    }
}
