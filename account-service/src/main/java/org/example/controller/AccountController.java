package org.example.controller;

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
    public ResponseEntity<Account> createAccount(@RequestParam Long userId,
                                                 @RequestParam BigDecimal initialBalance,
                                                 @RequestParam String currency) {
        return ResponseEntity.ok(accountService.createAccount(userId, initialBalance, currency));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Account> getAccount(@PathVariable Long id) {
        return accountService.getAccount(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Account>> getAccountsByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(accountService.getAccountsByUser(userId));
    }

    @PostMapping("/{id}/deposit")
    public ResponseEntity<Account> deposit(@PathVariable Long id,
                                           @RequestParam BigDecimal amount) {
        return ResponseEntity.ok(accountService.deposit(id, amount));
    }

    @PostMapping("/{id}/withdraw")
    public ResponseEntity<Account> withdraw(@PathVariable Long id,
                                            @RequestParam BigDecimal amount) {
        return ResponseEntity.ok(accountService.withdraw(id, amount));
    }
    @GetMapping("/{id}/validate")
    public ResponseEntity<Boolean> validateAccount(@PathVariable Long id,
                                                   @RequestParam Long userId) {
        boolean isOwner = accountService.validateAccountOwnership(id, userId);
        return ResponseEntity.ok(isOwner);
    }
    @GetMapping("/{id}/checksum")
    public ResponseEntity<Boolean> validateCheck(@PathVariable Long id,
                                                   @RequestParam BigDecimal sum) {
        boolean isOwner = accountService.validateAccountFund(id,sum);
        return ResponseEntity.ok(isOwner);
    }
}
