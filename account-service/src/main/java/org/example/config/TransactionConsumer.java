package org.example.config;

import org.example.model.TransactionEvent;
import org.example.service.AccountService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class TransactionConsumer {

    private final AccountService accountService;

    public TransactionConsumer(AccountService accountService) {
        this.accountService = accountService;
    }

    @KafkaListener(topics = "transactions-topic", groupId = "account-service")
    public void consume(TransactionEvent event) {
        if ("DEPOSIT".equals(event.getType())) {
            accountService.deposit(event.getAccountId(), event.getAmount());
        } else if ("WITHDRAW".equals(event.getType())) {
            accountService.withdraw(event.getAccountId(), event.getAmount());
        }
    }
}
