package org.example.kafka;

import org.example.model.TransactionEvent;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaProducerConfig {

    @Bean(name = "transactionProducerFactory")
    public ProducerFactory<String, TransactionEvent> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();

        // Use Kafka container service name in Docker Compose network
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "kafka:9092");

        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        // Optional: enable idempotent producer to avoid duplicates
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);

        // Optional: limit memory usage & retries
        configProps.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 32 * 1024 * 1024); // 32 MB
        configProps.put(ProducerConfig.RETRIES_CONFIG, 5);
        configProps.put(ProducerConfig.ACKS_CONFIG, "all"); // ensure durability

        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean(name = "transactionKafkaTemplate")
    public KafkaTemplate<String, TransactionEvent> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}
