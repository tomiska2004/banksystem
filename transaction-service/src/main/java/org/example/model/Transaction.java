package org.example.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import javax.management.ConstructorParameters;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Account ID is required")
    @Min(value = 1, message = "Account ID must be a positive number")
    private Long accountId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    @NotNull(message = "Type is required")
    @Pattern(regexp = "DEPOSIT|WITHDRAW|TRANSFER", message = "Type must be one of: DEPOSIT, WITHDRAW, TRANSFER")
    private String type;

    private LocalDateTime timestamp;

    private Long destinationAccountId;

    public Transaction(Long transactionId, Long accountId, BigDecimal amount, String type, LocalDateTime stamp) {
        this.id = transactionId;
        this.accountId = accountId;
        this.amount = amount;
        this.type = type;
        this.timestamp = stamp;
    }
}
