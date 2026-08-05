// package com.example.payments.controller;

// import static org.mockito.Mockito.when;
// import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
// import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
// import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// import com.example.payments.model.entity.Account;
// import com.example.payments.repository.AccountRepository;
// import com.example.payments.security.CurrentUserService;
// import java.math.BigDecimal;
// import java.util.List;
// import org.junit.jupiter.api.Test;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
// import org.springframework.boot.test.mock.mockito.MockBean;
// import org.springframework.test.web.servlet.MockMvc;

// @WebMvcTest(AccountController.class)
// class AccountControllerTest {

//     @Autowired
//     private MockMvc mockMvc;

//     @MockBean
//     private AccountRepository accountRepository;

//     @MockBean
//     private CurrentUserService currentUserService;

//     @Test
//     void myAccounts_returns_accounts_for_current_user() throws Exception {
//         when(currentUserService.getCurrentUserId()).thenReturn(1L);

//         Account account = new Account();
//         account.setAccountNumber("ACC-001");
//         account.setDisplayName("Main Account");
//         account.setBalance(new BigDecimal("1500.00"));

//         when(accountRepository.findByUser_IdOrderByAccountNumberAsc(1L))
//             .thenReturn(List.of(account));

//         mockMvc.perform(get("/api/accounts/me"))
//             .andExpect(status().isOk())
//             .andExpect(jsonPath("$[0].accountNumber").value("ACC-001"))
//             .andExpect(jsonPath("$[0].displayName").value("Main Account"))
//             .andExpect(jsonPath("$[0].balance").value(1500.00));
//     }

//     @Test
//     void myAccounts_returns_empty_list_when_no_accounts() throws Exception {
//         when(currentUserService.getCurrentUserId()).thenReturn(2L);
//         when(accountRepository.findByUser_IdOrderByAccountNumberAsc(2L))
//             .thenReturn(List.of());

//         mockMvc.perform(get("/api/accounts/me"))
//             .andExpect(status().isOk())
//             .andExpect(jsonPath("$.length()").value(0));
//     }

//     @Test
//     void myAccounts_returns_multiple_accounts_ordered_by_account_number() throws Exception {
//         when(currentUserService.getCurrentUserId()).thenReturn(3L);

//         Account account1 = new Account();
//         account1.setAccountNumber("ACC-001");
//         account1.setDisplayName("Checking");
//         account1.setBalance(new BigDecimal("500.00"));

//         Account account2 = new Account();
//         account2.setAccountNumber("ACC-002");
//         account2.setDisplayName("Savings");
//         account2.setBalance(new BigDecimal("2000.00"));

//         when(accountRepository.findByUser_IdOrderByAccountNumberAsc(3L))
//             .thenReturn(List.of(account1, account2));

//         mockMvc.perform(get("/api/accounts/me"))
//             .andExpect(status().isOk())
//             .andExpect(jsonPath("$.length()").value(2))
//             .andExpect(jsonPath("$[0].accountNumber").value("ACC-001"))
//             .andExpect(jsonPath("$[1].accountNumber").value("ACC-002"));
//     }
// }
package com.example.payments.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.payments.dto.AccountResponse;
import com.example.payments.model.entity.Account;
import com.example.payments.repository.AccountRepository;
import com.example.payments.security.CurrentUserService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AccountControllerTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private CurrentUserService currentUserService;

    private AccountController accountController;

    @BeforeEach
    void setUp() {
        accountController = new AccountController(accountRepository, currentUserService);
    }

    @Test
    void returns_accounts_for_current_user_with_mapped_fields() {
        Account account = new Account();
        account.setAccountNumber("ACC-101");
        account.setDisplayName("Main Wallet");
        account.setBalance(new BigDecimal("1250.75"));

        when(currentUserService.getCurrentUserId()).thenReturn(9L);
        when(accountRepository.findByUser_IdOrderByAccountNumberAsc(9L)).thenReturn(List.of(account));

        List<AccountResponse> result = accountController.myAccounts();

        assertEquals(1, result.size());
        assertEquals("ACC-101", result.get(0).getAccountNumber());
        assertEquals("Main Wallet", result.get(0).getDisplayName());
        assertEquals(new BigDecimal("1250.75"), result.get(0).getBalance());
        verify(accountRepository).findByUser_IdOrderByAccountNumberAsc(9L);
    }

    @Test
    void returns_empty_list_when_current_user_has_no_accounts() {
        when(currentUserService.getCurrentUserId()).thenReturn(15L);
        when(accountRepository.findByUser_IdOrderByAccountNumberAsc(15L)).thenReturn(List.of());

        List<AccountResponse> result = accountController.myAccounts();

        assertTrue(result.isEmpty());
        verify(accountRepository).findByUser_IdOrderByAccountNumberAsc(15L);
    }
}

