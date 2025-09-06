package org.example.config;

import org.example.model.TransactionEvent;
import org.example.service.AccountService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class TransactionConsumer {

    private static final Logger logger = LoggerFactory.getLogger(TransactionConsumer.class);

    private final AccountService accountService;

    public TransactionConsumer(AccountService accountService) {
        this.accountService = accountService;
    }

    @KafkaListener(topics = "transaction-events", groupId = "account-service")
    public void consume(TransactionEvent event) {
        logger.info("Received transaction event: {}", event);

        if ("DEPOSIT".equals(event.getType())) {
            accountService.deposit(event.getAccountId(), event.getAmount());
            logger.info("Processed DEPOSIT of {} for account {}", event.getAmount(), event.getAccountId());
        } else if ("WITHDRAW".equals(event.getType())) {
            accountService.withdraw(event.getAccountId(), event.getAmount());
            logger.info("Processed WITHDRAW of {} for account {}", event.getAmount(), event.getAccountId());
        } else {
            logger.warn("Unknown transaction type: {}", event.getType());
        }
    }
}
