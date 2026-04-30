package com.homework.banking.service;

import com.homework.banking.dto.BalanceResponse;
import com.homework.banking.dto.InterestResponse;
import com.homework.banking.dto.SummaryResponse;
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
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private TransactionStore store;

    @InjectMocks
    private AccountService service;

    // ── getBalance ─────────────────────────────────────────────────────────

    @Test
    void getBalance_noTransactions_returnsEmptyBalances() {
        when(store.findAll()).thenReturn(List.of());

        BalanceResponse result = service.getBalance("ACC-12345");

        assertThat(result.accountId()).isEqualTo("ACC-12345");
        assertThat(result.balances()).isEmpty();
    }

    @Test
    void getBalance_deposit_increasesToAccountBalance() {
        when(store.findAll()).thenReturn(List.of(
                tx(null, "ACC-12345", "1000.00", "USD", TransactionType.DEPOSIT, TransactionStatus.COMPLETED)
        ));

        BalanceResponse result = service.getBalance("ACC-12345");

        assertThat(result.balances().get("USD")).isEqualByComparingTo("1000.00");
    }

    @Test
    void getBalance_withdrawal_decreasesFromAccountBalance() {
        when(store.findAll()).thenReturn(List.of(
                tx("ACC-12345", null, "200.00", "USD", TransactionType.WITHDRAWAL, TransactionStatus.COMPLETED)
        ));

        BalanceResponse result = service.getBalance("ACC-12345");

        assertThat(result.balances().get("USD")).isEqualByComparingTo("-200.00");
    }

    @Test
    void getBalance_transfer_decreasesSenderAndIncreasesReceiver() {
        Transaction transfer = tx("ACC-12345", "ACC-67890", "300.00", "USD", TransactionType.TRANSFER, TransactionStatus.COMPLETED);
        when(store.findAll()).thenReturn(List.of(transfer));

        assertThat(service.getBalance("ACC-12345").balances().get("USD")).isEqualByComparingTo("-300.00");
        assertThat(service.getBalance("ACC-67890").balances().get("USD")).isEqualByComparingTo("300.00");
    }

    @Test
    void getBalance_pendingTransactionIgnored() {
        when(store.findAll()).thenReturn(List.of(
                tx(null, "ACC-12345", "500.00", "USD", TransactionType.DEPOSIT, TransactionStatus.PENDING)
        ));

        assertThat(service.getBalance("ACC-12345").balances()).isEmpty();
    }

    @Test
    void getBalance_multiCurrency_groupedByCurrencyKey() {
        when(store.findAll()).thenReturn(List.of(
                tx(null, "ACC-12345", "1000.00", "USD", TransactionType.DEPOSIT, TransactionStatus.COMPLETED),
                tx(null, "ACC-12345",  "500.50", "EUR", TransactionType.DEPOSIT, TransactionStatus.COMPLETED)
        ));

        BalanceResponse result = service.getBalance("ACC-12345");

        assertThat(result.balances().get("USD")).isEqualByComparingTo("1000.00");
        assertThat(result.balances().get("EUR")).isEqualByComparingTo("500.50");
    }

    @Test
    void getBalance_mixedTransactions_netBalanceCorrect() {
        when(store.findAll()).thenReturn(List.of(
                tx(null,       "ACC-12345", "1000.00", "USD", TransactionType.DEPOSIT,    TransactionStatus.COMPLETED),
                tx("ACC-12345", null,        "200.00", "USD", TransactionType.WITHDRAWAL, TransactionStatus.COMPLETED),
                tx("ACC-12345", "ACC-67890", "300.00", "USD", TransactionType.TRANSFER,   TransactionStatus.COMPLETED)
        ));

        BalanceResponse result = service.getBalance("ACC-12345");

        // 1000 - 200 - 300 = 500
        assertThat(result.balances().get("USD")).isEqualByComparingTo("500.00");
    }

    // ── getInterest ────────────────────────────────────────────────────────

    @Test
    void getInterest_calculatesSimpleInterestCorrectly() {
        // balance = 700 USD; interest = 700 * 0.05 * 30 / 365 = 2.876... → 2.88
        when(store.findAll()).thenReturn(List.of(
                tx(null,       "ACC-12345", "1000.00", "USD", TransactionType.DEPOSIT,    TransactionStatus.COMPLETED),
                tx("ACC-12345", null,        "300.00", "USD", TransactionType.WITHDRAWAL, TransactionStatus.COMPLETED)
        ));

        InterestResponse result = service.getInterest("ACC-12345", 0.05, 30);

        assertThat(result.interest().get("USD")).isEqualByComparingTo("2.88");
        assertThat(result.projectedBalance().get("USD")).isEqualByComparingTo("702.88");
    }

    @Test
    void getInterest_negativeRate_throwsValidationExceptionRateField() {
        assertThatThrownBy(() -> service.getInterest("ACC-12345", -0.1, 30))
                .isInstanceOf(ValidationException.class)
                .satisfies(ex -> assertThat(((ValidationException) ex).getField()).isEqualTo("rate"));
    }

    @Test
    void getInterest_zeroDays_throwsValidationExceptionDaysField() {
        assertThatThrownBy(() -> service.getInterest("ACC-12345", 0.05, 0))
                .isInstanceOf(ValidationException.class)
                .satisfies(ex -> assertThat(((ValidationException) ex).getField()).isEqualTo("days"));
    }

    // ── getSummary ─────────────────────────────────────────────────────────

    @Test
    void getSummary_calculatesTotalsAndCountCorrectly() {
        when(store.findAll()).thenReturn(List.of(
                tx(null,        "ACC-12345", "1000.00", "USD", TransactionType.DEPOSIT,    TransactionStatus.COMPLETED),
                tx("ACC-12345", null,         "200.00", "USD", TransactionType.WITHDRAWAL, TransactionStatus.COMPLETED),
                tx("ACC-12345", "ACC-67890",  "100.00", "USD", TransactionType.TRANSFER,   TransactionStatus.COMPLETED)
        ));

        SummaryResponse result = service.getSummary("ACC-12345");

        assertThat(result.transactionCount()).isEqualTo(3);
        assertThat(result.totalDeposits().get("USD")).isEqualByComparingTo("1000.00");
        // withdrawal 200 + transfer-out 100 = 300
        assertThat(result.totalWithdrawals().get("USD")).isEqualByComparingTo("300.00");
        assertThat(result.mostRecentTransaction()).isNotNull();
    }

    @Test
    void getSummary_multiCurrency_groupedByCurrency() {
        when(store.findAll()).thenReturn(List.of(
                tx(null, "ACC-12345", "1000.00", "USD", TransactionType.DEPOSIT, TransactionStatus.COMPLETED),
                tx(null, "ACC-12345",  "500.00", "EUR", TransactionType.DEPOSIT, TransactionStatus.COMPLETED),
                tx("ACC-12345", null,   "200.00", "USD", TransactionType.WITHDRAWAL, TransactionStatus.COMPLETED)
        ));

        SummaryResponse result = service.getSummary("ACC-12345");

        assertThat(result.totalDeposits().get("USD")).isEqualByComparingTo("1000.00");
        assertThat(result.totalDeposits().get("EUR")).isEqualByComparingTo("500.00");
        assertThat(result.totalWithdrawals().get("USD")).isEqualByComparingTo("200.00");
        assertThat(result.totalWithdrawals().get("EUR")).isNull();
    }

    @Test
    void getSummary_pendingTransactionsNotCounted() {
        when(store.findAll()).thenReturn(List.of(
                tx(null, "ACC-12345", "500.00", "USD", TransactionType.DEPOSIT, TransactionStatus.PENDING)
        ));

        SummaryResponse result = service.getSummary("ACC-12345");

        assertThat(result.transactionCount()).isZero();
        assertThat(result.totalDeposits()).isEmpty();
        assertThat(result.totalWithdrawals()).isEmpty();
    }

    // ── helpers ────────────────────────────────────────────────────────────

    private Transaction tx(String from, String to, String amount, String currency,
                           TransactionType type, TransactionStatus status) {
        return new Transaction(UUID.randomUUID().toString(), from, to,
                new BigDecimal(amount), currency, type, Instant.now(), status);
    }
}
