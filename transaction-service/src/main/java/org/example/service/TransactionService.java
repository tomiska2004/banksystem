package org.example.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.kafka.TransactionProducer;
import org.example.model.Transaction;
import org.example.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final TransactionProducer transactionProducer;
    private final ObjectMapper objectMapper;

    public TransactionService(TransactionRepository transactionRepository,
                              TransactionProducer transactionProducer,
                              ObjectMapper objectMapper) {
        this.transactionRepository = transactionRepository;
        this.transactionProducer = transactionProducer;
        this.objectMapper = objectMapper;
    }

    public Transaction createTransaction(Transaction tx) {
        Transaction saved = transactionRepository.save(tx);

        try {
            String msg = objectMapper.writeValueAsString(saved);
            transactionProducer.sendTransaction("transactions-topic", msg);
        } catch (Exception e) {
            e.printStackTrace(); // log error
        }

        return saved;
    }

    public List<Transaction> getTransactionsByAccount(Long accountId) {
        return transactionRepository.findByAccountId(accountId);
    }
}
