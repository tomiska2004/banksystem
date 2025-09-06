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
        logger.info("Received transaction event: {}", event.getDestinationAccountId());

        try {
            if ("DEPOSIT".equals(event.getType())) {
                accountService.deposit(event.getAccountId(), event.getAmount());
                logger.info("Processed DEPOSIT of {} for account {}", event.getAmount(), event.getAccountId());
            } else if ("WITHDRAW".equals(event.getType())) {
                accountService.withdraw(event.getAccountId(), event.getAmount());
                logger.info("Processed WITHDRAW of {} for account {}", event.getAmount(), event.getAccountId());
            } else if ("TRANSFER".equals(event.getType())) {
                if (event.getDestinationAccountId() == null) {
                    logger.error("TRANSFER event missing destinationAccountId: {}", event);
                    return;
                }

                // 1. Withdraw from source
                accountService.withdraw(event.getAccountId(), event.getAmount());
                logger.info("Processed WITHDRAW of {} from source account {}", event.getAmount(), event.getAccountId());

                // 2. Deposit to destination
                accountService.deposit(event.getDestinationAccountId(), event.getAmount());
                logger.info("Processed DEPOSIT of {} to destination account {}", event.getAmount(), event.getDestinationAccountId());

                logger.info("Processed TRANSFER of {} from account {} to account {}",
                        event.getAmount(), event.getAccountId(), event.getDestinationAccountId());
            } else {
                logger.warn("Unknown transaction type: {}", event.getType());
            }
        } catch (Exception e) {
            logger.error("Error processing transaction event: {}", event, e);
            throw e;
        }
    }
}
