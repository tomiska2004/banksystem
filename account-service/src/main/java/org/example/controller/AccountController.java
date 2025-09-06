package org.example.controller;

import jakarta.validation.constraints.*;
import org.example.model.Account;
import org.example.service.AccountService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    public ResponseEntity<Account> createAccount(
            @RequestParam @Min(1) Long userId,
            @RequestParam @DecimalMin(value = "0.00", message = "Initial balance cannot be negative") BigDecimal initialBalance,
            @RequestParam @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a 3-letter ISO code (e.g., USD, EUR)") String currency) {

        return ResponseEntity.ok(accountService.createAccount(userId, initialBalance, currency));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Account> getAccount(
            @PathVariable @Min(1) Long id) {
        return accountService.getAccount(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Account>> getAccountsByUser(
            @PathVariable @Min(1) Long userId) {
        return ResponseEntity.ok(accountService.getAccountsByUser(userId));
    }

    @PostMapping("/{id}/deposit")
    public ResponseEntity<Account> deposit(
            @PathVariable @Min(1) Long id,
            @RequestParam @DecimalMin(value = "0.01", message = "Deposit amount must be greater than 0") BigDecimal amount) {

        return ResponseEntity.ok(accountService.deposit(id, amount));
    }

    @PostMapping("/{id}/withdraw")
    public ResponseEntity<Account> withdraw(
            @PathVariable @Min(1) Long id,
            @RequestParam @DecimalMin(value = "0.01", message = "Withdrawal amount must be greater than 0") BigDecimal amount) {

        return ResponseEntity.ok(accountService.withdraw(id, amount));
    }

    @GetMapping("/{id}/validate")
    public ResponseEntity<Boolean> validateAccount(
            @PathVariable @Min(1) Long id,
            @RequestParam @Min(1) Long userId) {

        boolean isOwner = accountService.validateAccountOwnership(id, userId);
        return ResponseEntity.ok(isOwner);
    }

    @GetMapping("/{id}/checksum")
    public ResponseEntity<Boolean> validateCheck(
            @PathVariable @Min(1) Long id,
            @RequestParam @DecimalMin(value = "0.00", message = "Sum cannot be negative") BigDecimal sum) {

        boolean hasSufficientFunds = accountService.validateAccountFund(id, sum);
        return ResponseEntity.ok(hasSufficientFunds);
    }
}