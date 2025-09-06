package org.example.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "accounts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "User ID is required")
    @Min(value = 1, message = "User ID must be positive")
    @Column(nullable = false)
    private Long userId;

    @NotNull(message = "Balance is required")
    @DecimalMin(value = "0.00", message = "Balance cannot be negative")
    @Column(nullable = false)
    private BigDecimal balance;

    @NotBlank(message = "Currency is required")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a 3-letter ISO code (e.g., USD, EUR)")
    @Column(nullable = false)
    private String currency;

    // Legacy method — consider deprecating or renaming
    public Long getUser() {
        return userId;
    }
}