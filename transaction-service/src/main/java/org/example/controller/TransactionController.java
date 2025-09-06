package org.example.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.example.config.JwtUtil;
import org.example.model.Transaction;
import org.example.service.TransactionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/transactions")
public class TransactionController {

    private final TransactionService transactionService;
    private final JwtUtil jwtUtil;

    public TransactionController(TransactionService transactionService, JwtUtil jwtUtil) {
        this.transactionService = transactionService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping
    public ResponseEntity<Transaction> createTransaction(
            @RequestBody Transaction tx,
            HttpServletRequest request) {

        String token = request.getHeader("Authorization").substring(7); // remove "Bearer "
        String username = jwtUtil.extractUsername(token);  // still keep username
        Long userId = jwtUtil.extractUserId(token);        // get userId

        return ResponseEntity.ok(transactionService.createTransaction(tx, username));
    }

    @GetMapping("/account/{accountId}")
    public ResponseEntity<List<Transaction>> getTransactionsByAccount(
            @PathVariable Long accountId,
            HttpServletRequest request) {

        String token = request.getHeader("Authorization").substring(7);
        Long userId = jwtUtil.extractUserId(token);

        return ResponseEntity.ok(transactionService.getTransactionsByAccount(accountId, userId.toString()));
    }
}
