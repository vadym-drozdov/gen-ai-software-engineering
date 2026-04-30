package com.homework.banking.model;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Lifecycle status of a transaction. Serializes to lowercase JSON strings.
 * Only {@code COMPLETED} transactions are counted toward account balances and summaries.
 */
public enum TransactionStatus {
    /** Transaction has been submitted but not yet settled. */
    PENDING("pending"),
    /** Transaction has settled and affects the account balance. */
    COMPLETED("completed"),
    /** Transaction was rejected or could not be processed. */
    FAILED("failed");

    private final String value;

    TransactionStatus(String value) {
        this.value = value;
    }

    /** Returns the lowercase JSON representation of this status. */
    @JsonValue
    public String getValue() {
        return value;
    }
}
