package com.example.payments.controller;

import com.example.payments.dto.CreatePaymentRequest;
import com.example.payments.dto.CreatePaymentResult;
import com.example.payments.dto.PaymentHistoryResponse;
import com.example.payments.dto.PaymentResponse;
import com.example.payments.dto.SendAttemptResponse;
import com.example.payments.dto.UpdateStatusRequest;
import com.example.payments.exception.ValidationException;
import com.example.payments.mapper.PaymentMapper;
import com.example.payments.model.ErrorCode;
import com.example.payments.model.PaymentStatus;
import com.example.payments.model.RoleType;
import com.example.payments.model.entity.Payment;
import com.example.payments.security.CurrentUserService;
import com.example.payments.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
@Tag(name = "Payments", description = "Payment processing operations")
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentMapper paymentMapper;
    private final CurrentUserService currentUserService;

    public PaymentController(PaymentService paymentService, PaymentMapper paymentMapper, CurrentUserService currentUserService) {
        this.paymentService = paymentService;
        this.paymentMapper = paymentMapper;
        this.currentUserService = currentUserService;
    }

    @Operation(summary = "Create payment")
    @ApiResponse(responseCode = "201", description = "Payment created")
    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(
        @RequestHeader("Idempotency-Key") String idempotencyKey,
        @RequestBody CreatePaymentRequest request
    ) {
        Long userId = currentUserService.getCurrentUserId();
        CreatePaymentResult result = paymentService.createPayment(userId, idempotencyKey, request);
        HttpStatus status = result.isCreated() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(paymentMapper.toResponse(result.getPayment()));
    }

    @Operation(summary = "Get payment by id")
    @GetMapping("/{id}")
    public PaymentResponse getPayment(@PathVariable Long id) {
        return paymentMapper.toResponse(paymentService.getPayment(currentUserService.getCurrentUserId(), id));
    }

    @Operation(summary = "List payments by account and role")
    @GetMapping
    public List<PaymentResponse> listPayments(
        @Parameter(description = "Optional active account") @RequestParam(required = false) String account,
        @Parameter(description = "Role: sent or received") @RequestParam String role,
        @Parameter(description = "Optional status filter") @RequestParam(required = false) PaymentStatus status
    ) {
        RoleType roleType = parseRole(role);
        Long userId = currentUserService.getCurrentUserId();
        return paymentService
            .listPayments(userId, account, roleType, status)
            .stream()
            .map(paymentMapper::toResponse)
            .toList();
    }

    @Operation(summary = "Get payment status history")
    @GetMapping("/{id}/history")
    public List<PaymentHistoryResponse> getPaymentHistory(@PathVariable Long id) {
        Long userId = currentUserService.getCurrentUserId();
        return paymentService
            .getPaymentHistory(userId, id)
            .stream()
            .map(paymentMapper::toHistoryResponse)
            .toList();
    }

    @Operation(summary = "Get payment send attempts")
    @GetMapping("/{id}/attempts")
    public List<SendAttemptResponse> getSendAttempts(@PathVariable Long id) {
        Long userId = currentUserService.getCurrentUserId();
        return paymentService
            .getSendAttempts(userId, id)
            .stream()
            .map(paymentMapper::toAttemptResponse)
            .toList();
    }

    @Operation(summary = "Manually transition payment status")
    @PatchMapping("/{id}/status")
    public PaymentResponse transitionStatus(@PathVariable Long id, @RequestBody UpdateStatusRequest request) {
        if (request == null || request.getNewStatus() == null) {
            throw new ValidationException(ErrorCode.VALIDATION_FAILED, "newStatus is required");
        }
        Payment payment = paymentService.transitionStatus(
            currentUserService.getCurrentUserId(),
            id,
            request.getNewStatus(),
            request.getReason()
        );
        return paymentMapper.toResponse(payment);
    }

    @Operation(summary = "Simulate send step")
    @PostMapping("/{id}/send")
    public ResponseEntity<Void> sendPayment(@PathVariable Long id) {
        paymentService.sendPayment(currentUserService.getCurrentUserId(), id);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    private RoleType parseRole(String role) {
        if (role == null || role.isBlank()) {
            throw new ValidationException(ErrorCode.VALIDATION_FAILED, "role is required (sent or received)");
        }

        return switch (role.trim().toUpperCase()) {
            case "SENT" -> RoleType.SENT;
            case "RECEIVED" -> RoleType.RECEIVED;
            default -> throw new ValidationException(ErrorCode.VALIDATION_FAILED, "role must be sent or received");
        };
    }
}
