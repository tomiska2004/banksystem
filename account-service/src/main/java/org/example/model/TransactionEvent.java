package org.example.model;

import java.math.BigDecimal;

public class TransactionEvent {
    private Long transactionId;
    private Long accountId;
    private BigDecimal amount;
    private String type;   // "DEPOSIT", "WITHDRAW", "TRANSFER"
    private String timestamp;
    private Long destinationAccountId;


    public TransactionEvent() {}

    public TransactionEvent(Long transactionId, Long accountId, BigDecimal amount, String type, String timestamp, Long destinationAccountId) {
        this.transactionId = transactionId;
        this.accountId = accountId;
        this.amount = amount;
        this.type = type;
        this.timestamp = timestamp;
        this.destinationAccountId=destinationAccountId;
    }

    public Long getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(Long transactionId) {
        this.transactionId = transactionId;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Long getDestinationAccountId() {
        return destinationAccountId;
    }
}
