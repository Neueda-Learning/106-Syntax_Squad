package com.example.payments.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.payments.dto.PayeeRequest;
import com.example.payments.dto.PayeeResponse;
import com.example.payments.model.entity.Payee;
import com.example.payments.security.CurrentUserService;
import com.example.payments.service.PayeeService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class PayeeControllerTest {

    @Mock
    private PayeeService payeeService;

    @Mock
    private CurrentUserService currentUserService;

    private PayeeController payeeController;

    @BeforeEach
    void setUp() {
        payeeController = new PayeeController(payeeService, currentUserService);
    }

    @Test
    void create_payee_returns_created_response_with_mapped_fields() {
        PayeeRequest request = new PayeeRequest();
        request.setPayeeAccountNumber("ACC-200");
        request.setNickname("Rent");

        Payee payee = new Payee();
        payee.setId(8L);
        payee.setPayeeAccountNumber("ACC-200");
        payee.setNickname("Rent");
        payee.setCreatedAt(LocalDateTime.of(2026, 8, 5, 10, 0));

        when(currentUserService.getCurrentUserId()).thenReturn(3L);
        when(payeeService.createPayee(3L, request)).thenReturn(payee);

        ResponseEntity<PayeeResponse> response = payeeController.createPayee(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(8L, response.getBody().getId());
        assertEquals("ACC-200", response.getBody().getPayeeAccountNumber());
        assertEquals("Rent", response.getBody().getNickname());
        assertEquals(LocalDateTime.of(2026, 8, 5, 10, 0), response.getBody().getCreatedAt());
        verify(payeeService).createPayee(3L, request);
    }

    @Test
    void list_payees_returns_mapped_responses_for_current_user() {
        Payee first = new Payee();
        first.setId(11L);
        first.setPayeeAccountNumber("ACC-111");
        first.setNickname("Utilities");
        first.setCreatedAt(LocalDateTime.of(2026, 8, 1, 9, 0));

        Payee second = new Payee();
        second.setId(12L);
        second.setPayeeAccountNumber("ACC-222");
        second.setNickname("Savings");
        second.setCreatedAt(LocalDateTime.of(2026, 8, 2, 9, 0));

        when(currentUserService.getCurrentUserId()).thenReturn(4L);
        when(payeeService.listPayees(4L)).thenReturn(List.of(first, second));

        List<PayeeResponse> result = payeeController.listPayees();

        assertEquals(2, result.size());
        assertEquals(11L, result.get(0).getId());
        assertEquals("ACC-111", result.get(0).getPayeeAccountNumber());
        assertEquals("Utilities", result.get(0).getNickname());
        assertEquals(12L, result.get(1).getId());
        assertEquals("ACC-222", result.get(1).getPayeeAccountNumber());
        assertEquals("Savings", result.get(1).getNickname());
        verify(payeeService).listPayees(4L);
    }

    @Test
    void list_payees_returns_empty_list_when_user_has_none() {
        when(currentUserService.getCurrentUserId()).thenReturn(17L);
        when(payeeService.listPayees(17L)).thenReturn(List.of());

        List<PayeeResponse> result = payeeController.listPayees();

        assertTrue(result.isEmpty());
        verify(payeeService).listPayees(17L);
    }

    @Test
    void delete_payee_returns_no_content_after_successful_deletion() {
        when(currentUserService.getCurrentUserId()).thenReturn(5L);

        ResponseEntity<Void> response = payeeController.deletePayee(21L);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertNull(response.getBody());
        verify(payeeService).deletePayee(5L, 21L);
    }
}

