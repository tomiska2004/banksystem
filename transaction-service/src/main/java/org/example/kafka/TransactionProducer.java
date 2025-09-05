package org.example.kafka;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class TransactionProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public TransactionProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendTransaction(String topic, String message) {
        kafkaTemplate.send(topic, message);
    }
}
