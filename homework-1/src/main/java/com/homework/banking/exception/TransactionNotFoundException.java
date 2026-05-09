package com.homework.banking.exception;

/**
 * Thrown when a transaction lookup by ID finds no matching record in the store.
 * Mapped to HTTP 404 by {@link GlobalExceptionHandler}.
 */
public class TransactionNotFoundException extends RuntimeException {

    /**
     * @param id the transaction ID that was not found
     */
    public TransactionNotFoundException(String id) {
        super("Transaction not found: " + id);
    }
}
