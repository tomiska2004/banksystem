package org.example.service;

import org.example.model.Transaction;
import org.example.model.TransactionEvent;
import org.example.repository.TransactionRepository;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

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
        // Step 1: Validate ownership of source account
        validateAccountOwnership(tx.getAccountId(), userId);

        // Step 2: Validate based on transaction type
        if ("TRANSFER".equals(tx.getType())) {
            validateForTransfer(tx);
        } else if ("WITHDRAW".equals(tx.getType())) {
            validateForWithdraw(tx);
        }


        // Step 3: Save transaction
        Transaction saved = transactionRepository.save(tx);
        logger.info("Transaction saved with ID: {}", saved.getId());

        // Step 4: Prepare and send Kafka event
        TransactionEvent event;

        if ("TRANSFER".equals(tx.getType()) && tx.getDestinationAccountId() != null) {
            logger.info("@@@@@@@@@@@@@@@DestinationAccountId%%%%%%%%%%%%{} second{}", tx.getDestinationAccountId(),saved.getDestinationAccountId());
            event = new TransactionEvent(
                    saved.getId(),
                    saved.getAccountId(),
                    saved.getAmount(),
                    saved.getType(),
                    saved.getTimestamp().toString(),
                    saved.getDestinationAccountId()
            );
        } else {
            event = new TransactionEvent(
                    saved.getId(),
                    saved.getAccountId(),
                    saved.getAmount(),
                    saved.getType(),
                    saved.getTimestamp().toString(),
                    null
            );
        }

        logger.info("Sending transaction event to Kafka: {}", event);
        kafkaTemplate.send("transaction-events", event);
        logger.info("Transaction event sent successfully for ID: {}", saved.getId());

        return saved;
    }

    public List<Transaction> getTransactionsByAccount(Long accountId, Long userId) {
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

        if (!Boolean.TRUE.equals(isOwner)) {
            logger.warn("Ownership validation failed for accountId: {} and userId: {}", accountId, userId);
            throw new RuntimeException("Unauthorized");
        }

        logger.info("Ownership validated successfully for accountId: {} and userId: {}", accountId, userId);

        List<Transaction> transactions = transactionRepository.findByAccountId(accountId);
        logger.info("Fetched {} transactions for accountId: {}", transactions.size(), accountId);
        return transactions;
    }

    // ──────────────────────────── PRIVATE HELPERS ─────────────────────────── //

    private void validateAccountOwnership(Long accountId, Long userId) {
        logger.info("Validating ownership for accountId: {} and userId: {}", accountId, userId);
        try {
            Boolean isOwner = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/{id}/validate")
                            .queryParam("userId", userId)
                            .build(accountId)
                    )
                    .retrieve()
                    .bodyToMono(Boolean.class)
                    .block();

            if (!Boolean.TRUE.equals(isOwner)) {
                logger.warn("Ownership validation failed for accountId: {} and userId: {}", accountId, userId);
                throw new RuntimeException("Unauthorized");
            }
            logger.info("Ownership validated successfully for accountId: {} and userId: {}", accountId, userId);
        } catch (Exception e) {
            logger.error("Error during ownership validation", e);
            throw new RuntimeException("Validation service unavailable");
        }
    }

    private void validateForWithdraw(Transaction tx) {
        logger.info("Validating sufficient funds for WITHDRAW of {} from account {}", tx.getAmount(), tx.getAccountId());
        try {
            Boolean hasSufficientFunds = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/{id}/checksum")
                            .queryParam("sum", tx.getAmount())
                            .build(tx.getAccountId())
                    )
                    .retrieve()
                    .bodyToMono(Boolean.class)
                    .block();

            if (!Boolean.TRUE.equals(hasSufficientFunds)) {
                logger.warn("Insufficient funds for account: {}", tx.getAccountId());
                throw new RuntimeException("NOT ENOUGH FUND");
            }
            logger.info("Sufficient funds validated for account: {}", tx.getAccountId());
        } catch (Exception e) {
            logger.error("Error during fund validation for account: {}", tx.getAccountId(), e);
            throw new RuntimeException("Validation service unavailable");
        }
    }

    private void validateForTransfer(Transaction tx) {
        if (tx.getDestinationAccountId() == null) {
            throw new RuntimeException("Destination account ID is required for transfers");
        }

        // Reuse withdraw validation for source account
        validateForWithdraw(tx);

        // Validate destination account EXISTS by calling GET /{id} — expect 200 OK
        logger.info("Validating existence of destination account: {}", tx.getDestinationAccountId());
        try {
            // We don't need the body — just check if 200 is returned
            webClient.get()
                    .uri("/{id}", tx.getDestinationAccountId())
                    .retrieve()
                    .toBodilessEntity() // ← No need to deserialize to Account!
                    .block();

            logger.info("Destination account exists: {}", tx.getDestinationAccountId());
        } catch (Exception e) {
            logger.warn("Destination account not found: {}", tx.getDestinationAccountId(), e);
            throw new RuntimeException("Destination account not found");
        }
    }
}