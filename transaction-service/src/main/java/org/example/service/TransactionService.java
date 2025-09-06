package org.example.service;

import org.example.model.Transaction;
import org.example.repository.TransactionRepository;
import org.example.model.TransactionEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final KafkaTemplate<String, TransactionEvent> kafkaTemplate;
    private final WebClient webClient;

    public TransactionService(TransactionRepository transactionRepository,
                              KafkaTemplate<String, TransactionEvent> kafkaTemplate,
                              WebClient.Builder webClientBuilder) {
        this.transactionRepository = transactionRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.webClient = webClientBuilder.baseUrl("http://localhost:8082/accounts").build();
    }

    public Transaction createTransaction(Transaction tx, String username) {
        // Call account-service to check ownership
        Boolean isOwner = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/{id}/validate")
                        .queryParam("username", username)
                        .build(tx.getAccountId())
                )
                .retrieve()
                .bodyToMono(Boolean.class)
                .block();

        if (isOwner == null || !isOwner) {
            throw new RuntimeException("Unauthorized");
        }

        // Save transaction
        Transaction saved = transactionRepository.save(tx);

        // Send event to Kafka
        TransactionEvent event = new TransactionEvent(
                saved.getId(),
                saved.getAccountId(),
                saved.getAmount(),
                saved.getType(),
                saved.getTimestamp().toString()
        );
        kafkaTemplate.send("transaction-events", event);

        return saved;
    }

    public List<Transaction> getTransactionsByAccount(Long accountId, String username) {
        // Call account-service for validation
        Object userId = null;
        Boolean isOwner = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/{accountId}/validate")
                        .queryParam("userId", userId)
                        .build(accountId)
                )
                .retrieve()
                .bodyToMono(Boolean.class)
                .block();
        if (isOwner == null || !isOwner) {
            throw new RuntimeException("Unauthorized");
        }

        return transactionRepository.findByAccountId(accountId);
    }
}
