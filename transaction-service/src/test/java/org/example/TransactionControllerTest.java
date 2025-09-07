package org.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.config.JwtUtil;
import org.example.model.Transaction;
import org.example.service.TransactionService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TransactionService transactionService;

    @MockBean
    private JwtUtil jwtUtil;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testCreateTransaction() throws Exception {
        Transaction inputTx = new Transaction();
        inputTx.setId(1L);
        inputTx.setAccountId(3L);
        inputTx.setAmount(BigDecimal.valueOf(50));
        inputTx.setType("DEPOSIT");

        Mockito.when(jwtUtil.extractUserId(any(String.class))).thenReturn(2L);
        Mockito.when(transactionService.createTransaction(any(Transaction.class), eq(2L)))
                .thenReturn(inputTx);

        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer dummy-token")
                        .content(objectMapper.writeValueAsString(inputTx)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.accountId").value(3))
                .andExpect(jsonPath("$.amount").value(50.0))
                .andExpect(jsonPath("$.type").value("DEPOSIT"));
    }

    @Test
    void testGetTransactionsByAccount() throws Exception {
        Transaction tx1 = new Transaction(1L, 3L, BigDecimal.valueOf(200), "DEPOSIT", null);
        Transaction tx2 = new Transaction(2L, 3L, BigDecimal.valueOf(50), "WITHDRAW", null);
        List<Transaction> transactions = Arrays.asList(tx1, tx2);

        Mockito.when(jwtUtil.extractUserId(any(String.class))).thenReturn(2L);
        Mockito.when(transactionService.getTransactionsByAccount(3L, 2L))
                .thenReturn(transactions);

        mockMvc.perform(get("/transactions/account/3")
                        .header("Authorization", "Bearer dummy-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[0].type").value("DEPOSIT"))
                .andExpect(jsonPath("$[1].type").value("WITHDRAW"));
    }

    @Test
    void testUnauthorizedRequest() throws Exception {
        Transaction inputTx = new Transaction();
        inputTx.setId(1L);

        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "invalidtoken")
                        .content(objectMapper.writeValueAsString(inputTx)))
                .andExpect(status().isBadRequest());
    }
}
