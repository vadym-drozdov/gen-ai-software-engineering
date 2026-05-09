package com.homework.banking.service;

import com.homework.banking.dto.CreateTransactionRequest;
import com.homework.banking.exception.TransactionNotFoundException;
import com.homework.banking.exception.ValidationException;
import com.homework.banking.model.Transaction;
import com.homework.banking.model.TransactionStatus;
import com.homework.banking.model.TransactionType;
import com.homework.banking.store.TransactionStore;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

/**
 * Core service for creating, querying, and exporting transactions.
 * Business-rule validation (account field requirements per type, transfer uniqueness)
 * is enforced here, after Bean Validation has already checked field formats.
 */
@Service
public class TransactionService {

    private final TransactionStore store;

    public TransactionService(TransactionStore store) {
        this.store = store;
    }

    /**
     * Validates and persists a new transaction. The transaction is always created
     * with {@link TransactionStatus#COMPLETED} status and a server-assigned UUID and timestamp.
     *
     * <p>Business rules per type:
     * <ul>
     *   <li>{@code deposit} — {@code toAccount} is required</li>
     *   <li>{@code withdrawal} — {@code fromAccount} is required</li>
     *   <li>{@code transfer} — both accounts required and must be distinct</li>
     * </ul>
     *
     * @param req the validated request DTO
     * @return the persisted transaction
     * @throws ValidationException if a business rule is violated (carries the offending field name)
     */
    public Transaction createTransaction(CreateTransactionRequest req) {
        validateBusinessRules(req);

        Transaction t = new Transaction(
                UUID.randomUUID().toString(),
                req.getFromAccount(),
                req.getToAccount(),
                req.getAmount(),
                req.getCurrency(),
                TransactionType.fromValue(req.getType()),
                Instant.now(),
                TransactionStatus.COMPLETED
        );
        return store.save(t);
    }

    /**
     * Returns all transactions matching the supplied filters. All filters are optional
     * and combined with AND semantics. Date boundaries are inclusive: a transaction
     * on the {@code to} date is included.
     *
     * @param accountId matches transactions where the account appears as either sender or receiver;
     *                  {@code null} to skip this filter
     * @param type      exact type to match ({@code "deposit"}, {@code "withdrawal"}, {@code "transfer"});
     *                  {@code null} to skip; invalid values throw {@link ValidationException}
     * @param from      earliest date (inclusive, UTC); {@code null} to skip
     * @param to        latest date (inclusive, UTC); {@code null} to skip
     * @return matching transactions sorted by timestamp descending
     * @throws ValidationException with {@code field = "type"} if {@code type} is not a valid transaction type
     */
    public List<Transaction> getAllTransactions(String accountId, String type, LocalDate from, LocalDate to) {
        if (type != null) {
            try {
                TransactionType.fromValue(type);
            } catch (IllegalArgumentException e) {
                throw new ValidationException("type", "Type must be one of: deposit, withdrawal, transfer");
            }
        }
        return store.findAll().stream()
                .filter(t -> accountId == null
                        || accountId.equals(t.getFromAccount())
                        || accountId.equals(t.getToAccount()))
                .filter(t -> type == null || t.getType().getValue().equals(type))
                .filter(t -> from == null
                        || !t.getTimestamp().isBefore(from.atStartOfDay(ZoneOffset.UTC).toInstant()))
                .filter(t -> to == null
                        || t.getTimestamp().isBefore(to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant()))
                .toList();
    }

    /**
     * Retrieves a single transaction by its ID.
     *
     * @param id the transaction ID to look up
     * @return the matching transaction
     * @throws TransactionNotFoundException if no transaction with that ID exists
     */
    public Transaction getById(String id) {
        return store.findById(id).orElseThrow(() -> new TransactionNotFoundException(id));
    }

    /**
     * Exports all transactions as a CSV string. Only {@code "csv"} is a supported format
     * (case-insensitive). Fields containing commas, quotes, or newlines are RFC 4180-quoted.
     *
     * @param format the export format; currently only {@code "csv"} is accepted
     * @return a UTF-8 CSV string with a header row followed by one row per transaction
     * @throws ValidationException with {@code field = "format"} if the format is unsupported
     */
    public String exportCsv(String format) {
        if (!"csv".equalsIgnoreCase(format)) {
            throw new ValidationException("format", "Only 'csv' format is supported");
        }
        StringBuilder sb = new StringBuilder();
        sb.append("id,fromAccount,toAccount,amount,currency,type,timestamp,status\n");
        for (Transaction t : store.findAll()) {
            sb.append(escape(t.getId())).append(',')
              .append(escape(t.getFromAccount())).append(',')
              .append(escape(t.getToAccount())).append(',')
              .append(t.getAmount()).append(',')
              .append(escape(t.getCurrency())).append(',')
              .append(escape(t.getType().getValue())).append(',')
              .append(t.getTimestamp()).append(',')
              .append(escape(t.getStatus().getValue())).append('\n');
        }
        return sb.toString();
    }

    private String escape(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private void validateBusinessRules(CreateTransactionRequest req) {
        String type = req.getType();
        if (type == null) return;

        switch (type) {
            case "deposit" -> {
                if (req.getToAccount() == null || req.getToAccount().isBlank()) {
                    throw new ValidationException("toAccount", "toAccount is required for deposit transactions");
                }
            }
            case "withdrawal" -> {
                if (req.getFromAccount() == null || req.getFromAccount().isBlank()) {
                    throw new ValidationException("fromAccount", "fromAccount is required for withdrawal transactions");
                }
            }
            case "transfer" -> {
                if (req.getFromAccount() == null || req.getFromAccount().isBlank()) {
                    throw new ValidationException("fromAccount", "fromAccount is required for transfer transactions");
                }
                if (req.getToAccount() == null || req.getToAccount().isBlank()) {
                    throw new ValidationException("toAccount", "toAccount is required for transfer transactions");
                }
                if (req.getFromAccount().equals(req.getToAccount())) {
                    throw new ValidationException("fromAccount", "fromAccount and toAccount must be different for transfer transactions");
                }
            }
        }
    }
}
