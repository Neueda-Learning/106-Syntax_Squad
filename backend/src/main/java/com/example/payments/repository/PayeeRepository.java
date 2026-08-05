package com.example.payments.repository;

import com.example.payments.model.entity.Payee;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PayeeRepository extends JpaRepository<Payee, Long> {

    List<Payee> findByOwnerUser_IdOrderByCreatedAtDesc(Long ownerUserId);

    Optional<Payee> findByOwnerUser_IdAndPayeeAccountNumber(Long ownerUserId, String payeeAccountNumber);

    Optional<Payee> findByIdAndOwnerUser_Id(Long id, Long ownerUserId);
}
