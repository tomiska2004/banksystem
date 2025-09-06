package org.example.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
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
            @Valid @RequestBody Transaction tx,  // Add @Valid
            HttpServletRequest request) {

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().build();
        }

        String token = authHeader.substring(7); // Remove "Bearer "
        Long userId = jwtUtil.extractUserId(token);

        if (userId == null || userId <= 0) {
            return ResponseEntity.badRequest().build();
        }

        Transaction createdTx = transactionService.createTransaction(tx, userId);
        return ResponseEntity.ok(createdTx);
    }

    @GetMapping("/account/{accountId}")
    public ResponseEntity<List<Transaction>> getTransactionsByAccount(
            @PathVariable @Min(1) Long accountId,  //  Validate path variable
            HttpServletRequest request) {

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().build();
        }

        String token = authHeader.substring(7);
        Long userId = jwtUtil.extractUserId(token);

        if (userId == null || userId <= 0) {
            return ResponseEntity.badRequest().build();
        }

        List<Transaction> transactions = transactionService.getTransactionsByAccount(accountId, userId);
        return ResponseEntity.ok(transactions);
    }
}