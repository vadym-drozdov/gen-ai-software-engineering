package com.homework.banking.service;

import com.homework.banking.dto.CreateTransactionRequest;
import com.homework.banking.exception.TransactionNotFoundException;
import com.homework.banking.exception.ValidationException;
import com.homework.banking.model.Transaction;
import com.homework.banking.model.TransactionStatus;
import com.homework.banking.model.TransactionType;
import com.homework.banking.store.TransactionStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionStore store;

    @InjectMocks
    private TransactionService service;

    // ── createTransaction ──────────────────────────────────────────────────

    @Test
    void createDeposit_savesTransactionWithCorrectFields() {
        when(store.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Transaction result = service.createTransaction(depositRequest("ACC-12345", "1000.00"));

        assertThat(result.getType()).isEqualTo(TransactionType.DEPOSIT);
        assertThat(result.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
        assertThat(result.getToAccount()).isEqualTo("ACC-12345");
        assertThat(result.getAmount()).isEqualByComparingTo("1000.00");
        assertThat(result.getId()).isNotBlank();
        assertThat(result.getTimestamp()).isNotNull();
        verify(store).save(any());
    }

    @Test
    void createTransfer_savesTransactionWithBothAccounts() {
        when(store.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Transaction result = service.createTransaction(transferRequest("ACC-11111", "ACC-22222", "250.00"));

        assertThat(result.getType()).isEqualTo(TransactionType.TRANSFER);
        assertThat(result.getFromAccount()).isEqualTo("ACC-11111");
        assertThat(result.getToAccount()).isEqualTo("ACC-22222");
    }

    @Test
    void createDeposit_withoutToAccount_throwsValidationExceptionFieldToAccount() {
        CreateTransactionRequest req = new CreateTransactionRequest();
        req.setType("deposit");
        req.setAmount(new BigDecimal("100"));
        req.setCurrency("USD");

        assertThatThrownBy(() -> service.createTransaction(req))
                .isInstanceOf(ValidationException.class)
                .satisfies(ex -> assertThat(((ValidationException) ex).getField()).isEqualTo("toAccount"));
    }

    @Test
    void createWithdrawal_withoutFromAccount_throwsValidationExceptionFieldFromAccount() {
        CreateTransactionRequest req = new CreateTransactionRequest();
        req.setType("withdrawal");
        req.setAmount(new BigDecimal("100"));
        req.setCurrency("USD");

        assertThatThrownBy(() -> service.createTransaction(req))
                .isInstanceOf(ValidationException.class)
                .satisfies(ex -> assertThat(((ValidationException) ex).getField()).isEqualTo("fromAccount"));
    }

    @Test
    void createTransfer_sameAccounts_throwsValidationExceptionFromAccount() {
        CreateTransactionRequest req = new CreateTransactionRequest();
        req.setType("transfer");
        req.setFromAccount("ACC-12345");
        req.setToAccount("ACC-12345");
        req.setAmount(new BigDecimal("100"));
        req.setCurrency("USD");

        assertThatThrownBy(() -> service.createTransaction(req))
                .isInstanceOf(ValidationException.class)
                .satisfies(ex -> assertThat(((ValidationException) ex).getField()).isEqualTo("fromAccount"));
    }

    // ── getAllTransactions ─────────────────────────────────────────────────

    @Test
    void getAllTransactions_noFilters_returnsAll() {
        when(store.findAll()).thenReturn(List.of(
                tx("ACC-A", "ACC-B", TransactionType.TRANSFER, Instant.now()),
                tx(null, "ACC-C", TransactionType.DEPOSIT, Instant.now())
        ));

        assertThat(service.getAllTransactions(null, null, null, null)).hasSize(2);
    }

    @Test
    void getAllTransactions_filterByAccountId_matchesFromOrToAccount() {
        Transaction matching = tx("ACC-12345", "ACC-99999", TransactionType.TRANSFER, Instant.now());
        Transaction other    = tx("ACC-AAAAA", "ACC-BBBBB", TransactionType.TRANSFER, Instant.now());
        when(store.findAll()).thenReturn(List.of(matching, other));

        List<Transaction> result = service.getAllTransactions("ACC-12345", null, null, null);

        assertThat(result).containsExactly(matching);
    }

    @Test
    void getAllTransactions_filterByType_returnsOnlyMatchingType() {
        Transaction deposit  = tx(null,      "ACC-12345", TransactionType.DEPOSIT,  Instant.now());
        Transaction transfer = tx("ACC-12345", "ACC-67890", TransactionType.TRANSFER, Instant.now());
        when(store.findAll()).thenReturn(List.of(deposit, transfer));

        List<Transaction> result = service.getAllTransactions(null, "deposit", null, null);

        assertThat(result).containsExactly(deposit);
    }

    @Test
    void getAllTransactions_filterByDateRange_excludesOutOfRange() {
        Transaction inRange    = tx("ACC-A", "ACC-B", TransactionType.TRANSFER, Instant.parse("2024-06-15T12:00:00Z"));
        Transaction outOfRange = tx("ACC-A", "ACC-B", TransactionType.TRANSFER, Instant.parse("2023-01-01T00:00:00Z"));
        when(store.findAll()).thenReturn(List.of(inRange, outOfRange));

        List<Transaction> result = service.getAllTransactions(
                null, null, LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31));

        assertThat(result).containsExactly(inRange);
    }

    @Test
    void getAllTransactions_invalidType_throwsValidationExceptionTypeField() {
        assertThatThrownBy(() -> service.getAllTransactions(null, "bogus", null, null))
                .isInstanceOf(ValidationException.class)
                .satisfies(ex -> assertThat(((ValidationException) ex).getField()).isEqualTo("type"));
    }

    @Test
    void getAllTransactions_toDateIsInclusive() {
        // Transaction exactly on the to-date boundary (end of day)
        Transaction onBoundary = tx("ACC-A", "ACC-B", TransactionType.TRANSFER, Instant.parse("2024-12-31T23:59:59Z"));
        Transaction afterBoundary = tx("ACC-A", "ACC-B", TransactionType.TRANSFER, Instant.parse("2025-01-01T00:00:00Z"));
        when(store.findAll()).thenReturn(List.of(onBoundary, afterBoundary));

        List<Transaction> result = service.getAllTransactions(
                null, null, null, LocalDate.of(2024, 12, 31));

        assertThat(result).containsExactly(onBoundary);
    }

    // ── getById ────────────────────────────────────────────────────────────

    @Test
    void getById_found_returnsTransaction() {
        Transaction tx = tx("ACC-A", "ACC-B", TransactionType.TRANSFER, Instant.now());
        when(store.findById(tx.getId())).thenReturn(Optional.of(tx));

        assertThat(service.getById(tx.getId())).isEqualTo(tx);
    }

    @Test
    void getById_notFound_throwsTransactionNotFoundException() {
        when(store.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById("missing"))
                .isInstanceOf(TransactionNotFoundException.class)
                .hasMessageContaining("missing");
    }

    // ── exportCsv ─────────────────────────────────────────────────────────

    @Test
    void exportCsv_validFormat_returnsCsvWithHeaderAndRows() {
        Transaction t = tx(null, "ACC-12345", TransactionType.DEPOSIT, Instant.parse("2026-01-01T00:00:00Z"));
        when(store.findAll()).thenReturn(List.of(t));

        String csv = service.exportCsv("csv");

        assertThat(csv).startsWith("id,fromAccount,toAccount,amount,currency,type,timestamp,status\n");
        assertThat(csv).contains("deposit");
        assertThat(csv).contains("completed");
        assertThat(csv).contains("ACC-12345");
    }

    @Test
    void exportCsv_caseInsensitiveFormat_accepted() {
        when(store.findAll()).thenReturn(List.of());
        assertThatNoException().isThrownBy(() -> service.exportCsv("CSV"));
    }

    @Test
    void exportCsv_invalidFormat_throwsValidationExceptionWithFormatField() {
        assertThatThrownBy(() -> service.exportCsv("xml"))
                .isInstanceOf(ValidationException.class)
                .satisfies(ex -> assertThat(((ValidationException) ex).getField()).isEqualTo("format"));
    }

    // ── helpers ────────────────────────────────────────────────────────────

    private CreateTransactionRequest depositRequest(String toAccount, String amount) {
        CreateTransactionRequest req = new CreateTransactionRequest();
        req.setType("deposit");
        req.setToAccount(toAccount);
        req.setAmount(new BigDecimal(amount));
        req.setCurrency("USD");
        return req;
    }

    private CreateTransactionRequest transferRequest(String from, String to, String amount) {
        CreateTransactionRequest req = new CreateTransactionRequest();
        req.setType("transfer");
        req.setFromAccount(from);
        req.setToAccount(to);
        req.setAmount(new BigDecimal(amount));
        req.setCurrency("USD");
        return req;
    }

    private Transaction tx(String from, String to, TransactionType type, Instant timestamp) {
        return new Transaction(UUID.randomUUID().toString(), from, to,
                new BigDecimal("100.00"), "USD", type, timestamp, TransactionStatus.COMPLETED);
    }
}
