package com.homework.banking.controller;

import com.homework.banking.dto.CreateTransactionRequest;
import com.homework.banking.model.Transaction;
import com.homework.banking.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * REST controller for the {@code /transactions} resource.
 * Handles transaction creation, listing with filters, single-record lookup, and CSV export.
 *
 * <p>Spring MVC resolves the literal path {@code /transactions/export} before the
 * variable path {@code /transactions/{id}}, so no ordering or special configuration is needed.</p>
 */
@RestController
@RequestMapping("/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    /**
     * Creates a new transaction. Bean Validation runs on the request body before
     * the service enforces business rules (account requirements per type).
     *
     * @param request the transaction details; must pass {@code @Valid} constraints
     * @return 201 Created with the persisted transaction in the body
     */
    @PostMapping
    public ResponseEntity<Transaction> create(@Valid @RequestBody CreateTransactionRequest request) {
        Transaction created = transactionService.createTransaction(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Lists all transactions, optionally filtered. All parameters are optional and ANDed together.
     * Date parameters use ISO-8601 format ({@code yyyy-MM-dd}) and are inclusive on both ends.
     *
     * @param accountId filters to transactions where this account is sender or receiver
     * @param type      filters by transaction type ({@code deposit}, {@code withdrawal}, {@code transfer})
     * @param from      earliest transaction date (inclusive)
     * @param to        latest transaction date (inclusive)
     * @return 200 OK with matching transactions sorted by timestamp descending
     */
    @GetMapping
    public ResponseEntity<List<Transaction>> getAll(
            @RequestParam(required = false) String accountId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(transactionService.getAllTransactions(accountId, type, from, to));
    }

    /**
     * Exports all transactions as a downloadable CSV file.
     * The response includes {@code Content-Disposition: attachment} so browsers prompt for download.
     *
     * @param format the export format; only {@code "csv"} (case-insensitive) is accepted
     * @return 200 OK with {@code Content-Type: text/csv} and the CSV body
     */
    @GetMapping("/export")
    public ResponseEntity<String> export(@RequestParam String format) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv; charset=UTF-8"));
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"transactions.csv\"");
        return ResponseEntity.ok().headers(headers).body(transactionService.exportCsv(format));
    }

    /**
     * Retrieves a single transaction by its ID.
     *
     * @param id the transaction ID (UUID)
     * @return 200 OK with the transaction body, or 404 if not found
     */
    @GetMapping("/{id}")
    public ResponseEntity<Transaction> getById(@PathVariable String id) {
        return ResponseEntity.ok(transactionService.getById(id));
    }
}
