package org.example.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.model.Transaction;
import org.example.repository.TransactionRepository;
import org.example.model.TransactionEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.Objects;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final KafkaTemplate<String, TransactionEvent> kafkaTemplate;
    private final WebClient webClient;
    private static final Logger logger = LoggerFactory.getLogger(TransactionService.class);

    public TransactionService(TransactionRepository transactionRepository,
                              KafkaTemplate<String, TransactionEvent> kafkaTemplate,
                              WebClient.Builder webClientBuilder) {
        this.transactionRepository = transactionRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.webClient = webClientBuilder
                .baseUrl("http://account-service:8080/accounts")
                .build();
        logger.info("TransactionService initialized");
    }

    public Transaction createTransaction(Transaction tx, Long userId) {
        String url = String.valueOf(webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/{id}/validate")
                        .queryParam("userId", userId)
                        .build(tx.getAccountId())
                ));

        logger.info("Constructed validation URL: {}", url);

        try {
            // Call account-service to check ownership
            logger.info("Calling account-service to validate ownership for accountId: {} and userId: {}", tx.getAccountId(), userId);
            Boolean isOwner = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/{id}/validate")
                            .queryParam("userId", userId)
                            .build(tx.getAccountId())
                    )
                    .retrieve()
                    .bodyToMono(Boolean.class)
                    .block();

            if (isOwner == null) {
                logger.warn("Ownership validation returned null for accountId: {} and userId: {}", tx.getAccountId(), userId);
            } else if (!isOwner) {
                logger.warn("Ownership validation failed for accountId: {} and userId: {}", tx.getAccountId(), userId);
                throw new RuntimeException("Unauthorized");
            } else {
                logger.info("Ownership validated successfully for accountId: {} and userId: {}", tx.getAccountId(), userId);
            }
        } catch (Exception e) {
            logger.error("Error during ownership validation for accountId: {} and userId: {}", tx.getAccountId(), userId, e);
            throw new RuntimeException("Validation service unavailable");
        }



        // Save transaction
        logger.info("Saving transaction: {}", tx);
        if(Objects.equals(tx.getType(), "WITHDRAW")){
            try {
                // Call account-service to check ownership
                logger.info("Calling account-service to validate ownership for accountId: {} and userId: {}", tx.getAccountId(), userId);
                Boolean sufFund = webClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/{id}/checksum")
                                .queryParam("sum", tx.getAmount())
                                .build(tx.getAccountId())
                        )
                        .retrieve()
                        .bodyToMono(Boolean.class)
                        .block();

                if (sufFund == null) {
                    logger.warn("Fund validation returned null for accountId: {} and userId: {}", tx.getAccountId(), userId);
                } else if (!sufFund) {
                    logger.warn("Fund validation failed for accountId: {} and userId: {}  FUND IS NOT ENOUGH", tx.getAccountId(), userId);
                    throw new RuntimeException("NOT ENOUGH FUND");
                } else {
                    logger.info("ENOUGH FUND validated successfully for accountId: {} and userId: {}", tx.getAccountId(), userId);
                }
            } catch (Exception e) {
                logger.error("Error during fund validation for accountId: {} and userId: {}", tx.getAccountId(), userId, e);
                throw new RuntimeException("Validation service unavailable/NOT ENOUGH FUND");
            }
        }

        Transaction saved = transactionRepository.save(tx);
        logger.info("Transaction saved with ID: {}", saved.getId());

        // Prepare event
        TransactionEvent event = new TransactionEvent(
                saved.getId(),
                saved.getAccountId(),
                saved.getAmount(),
                saved.getType(),
                saved.getTimestamp().toString()
        );



        // Send event to Kafka
        logger.info("Sending transaction event to Kafka for transaction ID: {}", saved.getId());
        kafkaTemplate.send("transaction-events", event);
        logger.info("Transaction event sent successfully for transaction ID: {}", saved.getId());

        return saved;
    }

    public List<Transaction> getTransactionsByAccount(Long accountId, Long userId) {
        // Call account-service for validation
        logger.info("Validating account ownership for accountId: {} and userId: {}", accountId, userId);
        Boolean isOwner;
        try {
            isOwner = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/{accountId}/validate")
                            .queryParam("userId", userId)
                            .build(accountId)
                    )
                    .retrieve()
                    .bodyToMono(Boolean.class)
                    .block();
        } catch (Exception e) {
            logger.error("Error during ownership validation for accountId: {} and userId: {}", accountId, userId, e);
            throw new RuntimeException("Validation service unavailable");
        }

        if (isOwner == null) {
            logger.warn("Ownership validation returned null for accountId: {} and userId: {}", accountId, userId);
            throw new RuntimeException("Unauthorized");
        } else if (!isOwner) {
            logger.warn("Ownership validation failed for accountId: {} and userId: {}", accountId, userId);
            throw new RuntimeException("Unauthorized");
        } else {
            logger.info("Ownership validated successfully for accountId: {} and userId: {}", accountId, userId);
        }

        // Fetch transactions
        List<Transaction> transactions = transactionRepository.findByAccountId(accountId);
        logger.info("Fetched {} transactions for accountId: {}", transactions.size(), accountId);
        return transactions;
    }
}
