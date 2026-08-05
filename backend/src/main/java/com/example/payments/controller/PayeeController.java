package com.example.payments.controller;

import com.example.payments.dto.PayeeRequest;
import com.example.payments.dto.PayeeResponse;
import com.example.payments.model.entity.Payee;
import com.example.payments.security.CurrentUserService;
import com.example.payments.service.PayeeService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payees")
public class PayeeController {

    private final PayeeService payeeService;
    private final CurrentUserService currentUserService;

    public PayeeController(PayeeService payeeService, CurrentUserService currentUserService) {
        this.payeeService = payeeService;
        this.currentUserService = currentUserService;
    }

    @PostMapping
    public ResponseEntity<PayeeResponse> createPayee(@RequestBody PayeeRequest request) {
        Long userId = currentUserService.getCurrentUserId();
        Payee payee = payeeService.createPayee(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(payee));
    }

    @GetMapping
    public List<PayeeResponse> listPayees() {
        Long userId = currentUserService.getCurrentUserId();
        return payeeService.listPayees(userId).stream().map(this::toResponse).toList();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePayee(@PathVariable Long id) {
        payeeService.deletePayee(currentUserService.getCurrentUserId(), id);
        return ResponseEntity.noContent().build();
    }

    private PayeeResponse toResponse(Payee payee) {
        PayeeResponse response = new PayeeResponse();
        response.setId(payee.getId());
        response.setPayeeAccountNumber(payee.getPayeeAccountNumber());
        response.setNickname(payee.getNickname());
        response.setCreatedAt(payee.getCreatedAt());
        return response;
    }
}
