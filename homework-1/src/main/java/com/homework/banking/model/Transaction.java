package com.homework.banking.model;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Immutable record of a single financial transaction. All fields are set at creation
 * and cannot be modified — use {@link com.homework.banking.service.TransactionService}
 * to create new transactions.
 *
 * <p>Account fields are nullable depending on type: deposits have no {@code fromAccount},
 * withdrawals have no {@code toAccount}, transfers have both.</p>
 */
public final class Transaction {

    private final String id;
    private final String fromAccount;
    private final String toAccount;
    private final BigDecimal amount;
    private final String currency;
    private final TransactionType type;
    private final Instant timestamp;
    private final TransactionStatus status;

    /**
     * Creates a fully-specified transaction. Prefer
     * {@link com.homework.banking.service.TransactionService#createTransaction} over
     * calling this constructor directly.
     *
     * @param id          unique transaction identifier (UUID)
     * @param fromAccount source account, or {@code null} for deposits
     * @param toAccount   destination account, or {@code null} for withdrawals
     * @param amount      positive monetary amount with at most 2 decimal places
     * @param currency    ISO 4217 currency code (e.g. {@code "USD"})
     * @param type        transaction classification
     * @param timestamp   UTC instant when the transaction was recorded
     * @param status      settlement status
     */
    public Transaction(String id, String fromAccount, String toAccount, BigDecimal amount,
                       String currency, TransactionType type, Instant timestamp, TransactionStatus status) {
        this.id = id;
        this.fromAccount = fromAccount;
        this.toAccount = toAccount;
        this.amount = amount;
        this.currency = currency;
        this.type = type;
        this.timestamp = timestamp;
        this.status = status;
    }

    public String getId() { return id; }
    public String getFromAccount() { return fromAccount; }
    public String getToAccount() { return toAccount; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public TransactionType getType() { return type; }
    public Instant getTimestamp() { return timestamp; }
    public TransactionStatus getStatus() { return status; }
}
