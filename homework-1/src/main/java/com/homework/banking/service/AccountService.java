package com.homework.banking.service;

import com.homework.banking.dto.BalanceResponse;
import com.homework.banking.dto.InterestResponse;
import com.homework.banking.dto.SummaryResponse;
import com.homework.banking.exception.ValidationException;
import com.homework.banking.model.Transaction;
import com.homework.banking.model.TransactionStatus;
import com.homework.banking.model.TransactionType;
import com.homework.banking.store.TransactionStore;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Account-level analytics: balance, activity summary, and interest projection.
 * All calculations consider only {@link TransactionStatus#COMPLETED} transactions.
 * Multi-currency amounts are kept separate and never aggregated across currencies.
 */
@Service
public class AccountService {

    private final TransactionStore store;

    public AccountService(TransactionStore store) {
        this.store = store;
    }

    /**
     * Computes the current net balance of an account per currency.
     * Incoming amounts (deposits, transfer credits) add to the balance;
     * outgoing amounts (withdrawals, transfer debits) subtract from it.
     *
     * @param accountId the account to query
     * @return a {@link BalanceResponse} with one entry per currency; empty if no completed transactions
     */
    public BalanceResponse getBalance(String accountId) {
        return new BalanceResponse(accountId, computeBalances(accountId));
    }

    /**
     * Produces an aggregated activity summary for an account, grouped by currency.
     * Incoming transfers are counted toward {@code totalDeposits};
     * outgoing transfers are counted toward {@code totalWithdrawals}.
     * Amounts are never summed across different currencies.
     *
     * @param accountId the account to summarize
     * @return a {@link SummaryResponse} with deposit totals per currency, withdrawal totals per currency,
     *         transaction count, and most recent transaction timestamp
     */
    public SummaryResponse getSummary(String accountId) {
        List<Transaction> all = store.findAll();
        Map<String, BigDecimal> totalDeposits = new HashMap<>();
        Map<String, BigDecimal> totalWithdrawals = new HashMap<>();
        long count = 0;
        Instant mostRecent = null;

        for (Transaction t : all) {
            boolean involves = accountId.equals(t.getFromAccount()) || accountId.equals(t.getToAccount());
            if (!involves || TransactionStatus.COMPLETED != t.getStatus()) continue;

            count++;
            if (mostRecent == null || t.getTimestamp().isAfter(mostRecent)) {
                mostRecent = t.getTimestamp();
            }

            String currency = t.getCurrency();
            if (TransactionType.DEPOSIT == t.getType() && accountId.equals(t.getToAccount())) {
                totalDeposits.merge(currency, t.getAmount(), BigDecimal::add);
            } else if (TransactionType.WITHDRAWAL == t.getType() && accountId.equals(t.getFromAccount())) {
                totalWithdrawals.merge(currency, t.getAmount(), BigDecimal::add);
            } else if (TransactionType.TRANSFER == t.getType()) {
                if (accountId.equals(t.getToAccount())) {
                    totalDeposits.merge(currency, t.getAmount(), BigDecimal::add);
                } else {
                    totalWithdrawals.merge(currency, t.getAmount(), BigDecimal::add);
                }
            }
        }

        return new SummaryResponse(accountId, totalDeposits, totalWithdrawals, count, mostRecent);
    }

    /**
     * Calculates a simple-interest projection for an account over a given period.
     * Formula per currency: {@code interest = balance × rate × days / 365},
     * rounded half-up to 2 decimal places.
     *
     * @param accountId the account to project
     * @param rate      annual interest rate as a decimal (must be &gt; 0, e.g. {@code 0.05} for 5%)
     * @param days      number of days to accrue interest (must be ≥ 1)
     * @return an {@link InterestResponse} with current balances, computed interest,
     *         and projected balances per currency
     * @throws ValidationException with {@code field = "rate"} if rate is not positive
     * @throws ValidationException with {@code field = "days"} if days is less than 1
     */
    public InterestResponse getInterest(String accountId, double rate, int days) {
        if (rate <= 0) {
            throw new ValidationException("rate", "Rate must be a positive number");
        }
        if (days < 1) {
            throw new ValidationException("days", "Days must be at least 1");
        }

        Map<String, BigDecimal> balances = computeBalances(accountId);
        Map<String, BigDecimal> interest = new HashMap<>();
        Map<String, BigDecimal> projected = new HashMap<>();

        BigDecimal rateDecimal = BigDecimal.valueOf(rate);
        BigDecimal daysDecimal = BigDecimal.valueOf(days);
        BigDecimal yearDays = BigDecimal.valueOf(365);

        for (Map.Entry<String, BigDecimal> entry : balances.entrySet()) {
            BigDecimal bal = entry.getValue();
            BigDecimal interestAmount = bal.multiply(rateDecimal)
                    .multiply(daysDecimal)
                    .divide(yearDays, 6, RoundingMode.HALF_UP)
                    .setScale(2, RoundingMode.HALF_UP);
            interest.put(entry.getKey(), interestAmount);
            projected.put(entry.getKey(), bal.add(interestAmount));
        }

        return new InterestResponse(accountId, balances, rate, days, interest, projected);
    }

    /**
     * Computes net balances per currency for an account by scanning all completed transactions.
     * Credits (toAccount matches) are added; debits (fromAccount matches) are subtracted.
     */
    private Map<String, BigDecimal> computeBalances(String accountId) {
        Map<String, BigDecimal> balances = new HashMap<>();

        for (Transaction t : store.findAll()) {
            if (TransactionStatus.COMPLETED != t.getStatus()) continue;

            String currency = t.getCurrency();

            if (accountId.equals(t.getToAccount())) {
                balances.merge(currency, t.getAmount(), BigDecimal::add);
            }
            if (accountId.equals(t.getFromAccount())) {
                balances.merge(currency, t.getAmount().negate(), BigDecimal::add);
            }
        }

        return balances;
    }
}
