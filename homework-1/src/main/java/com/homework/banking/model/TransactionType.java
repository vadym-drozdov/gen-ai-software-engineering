package com.homework.banking.model;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Supported transaction types. Serializes to lowercase JSON strings
 * ({@code "deposit"}, {@code "withdrawal"}, {@code "transfer"}).
 */
public enum TransactionType {
    /** Funds added to an account (requires {@code toAccount}). */
    DEPOSIT("deposit"),
    /** Funds removed from an account (requires {@code fromAccount}). */
    WITHDRAWAL("withdrawal"),
    /** Funds moved between two distinct accounts (requires both accounts). */
    TRANSFER("transfer");

    private final String value;

    TransactionType(String value) {
        this.value = value;
    }

    /** Returns the lowercase JSON representation of this type. */
    @JsonValue
    public String getValue() {
        return value;
    }

    /**
     * Resolves a type from its string representation (case-insensitive).
     *
     * @param value the string to resolve (e.g. {@code "deposit"}, {@code "TRANSFER"})
     * @return the matching {@code TransactionType}
     * @throws IllegalArgumentException if no matching type exists
     */
    public static TransactionType fromValue(String value) {
        for (TransactionType t : values()) {
            if (t.value.equalsIgnoreCase(value)) return t;
        }
        throw new IllegalArgumentException("Unknown transaction type: " + value);
    }
}
