package org.example.service;

import org.example.model.Account;
import org.example.repository.AccountRepository;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public AccountService(AccountRepository accountRepository,
                          KafkaTemplate<String, String> kafkaTemplate) {
        this.accountRepository = accountRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    public Account createAccount(Long userId, BigDecimal initialBalance, String currency) {
        Account account = Account.builder()
                .userId(userId)
                .balance(initialBalance)
                .currency(currency)
                .build();
        Account saved = accountRepository.save(account);

        // publish Kafka event
        kafkaTemplate.send("account-events", "Created account for userId " + userId);
        return saved;
    }

    public Optional<Account> getAccount(Long accountId) {
        return accountRepository.findById(accountId);
    }

    public List<Account> getAccountsByUser(Long userId) {
        return accountRepository.findByUserId(userId);
    }

    public Account deposit(Long accountId, BigDecimal amount) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));
        account.setBalance(account.getBalance().add(amount));
        Account saved = accountRepository.save(account);

        kafkaTemplate.send("account-events", "Deposited " + amount + " to account " + accountId);
        return saved;
    }

    public Account withdraw(Long accountId, BigDecimal amount) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));
        if (account.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient balance");
        }
        account.setBalance(account.getBalance().subtract(amount));
        Account saved = accountRepository.save(account);

        kafkaTemplate.send("account-events", "Withdrew " + amount + " from account " + accountId);
        return saved;
    }

    public boolean validateAccountOwnership(Long accountId, Long userId) {
        return accountRepository.findById(accountId)
                .map(account -> account.getUserId().equals(userId))
                .orElse(false);
    }
}
