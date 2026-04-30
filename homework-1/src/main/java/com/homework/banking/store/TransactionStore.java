package com.homework.banking.store;

import com.homework.banking.model.Transaction;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe in-memory store for {@link Transaction} objects, keyed by transaction ID.
 * All state is lost on application restart — there is no persistence layer.
 */
@Component
public class TransactionStore {

    private final ConcurrentHashMap<String, Transaction> store = new ConcurrentHashMap<>();

    /**
     * Persists a transaction, replacing any existing entry with the same ID.
     *
     * @param transaction the transaction to store
     * @return the same transaction (for method chaining)
     */
    public Transaction save(Transaction transaction) {
        store.put(transaction.getId(), transaction);
        return transaction;
    }

    /**
     * Looks up a transaction by its unique ID.
     *
     * @param id the transaction ID to look up
     * @return an {@link Optional} containing the transaction, or empty if not found
     */
    public Optional<Transaction> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    /**
     * Returns all stored transactions sorted by timestamp descending (most recent first).
     *
     * @return an unmodifiable list of all transactions
     */
    public List<Transaction> findAll() {
        return store.values().stream()
                .sorted(Comparator.comparing(Transaction::getTimestamp).reversed())
                .toList();
    }
}
