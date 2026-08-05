package com.example.payments.repository;

import com.example.payments.model.entity.Account;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, String> {

    boolean existsByAccountNumberAndUser_Id(String accountNumber, Long userId);

    List<Account> findByUser_IdOrderByAccountNumberAsc(Long userId);
}
