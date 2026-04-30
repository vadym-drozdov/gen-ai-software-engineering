package com.homework.banking.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/**
 * Aggregated activity summary for a single account, grouped by currency.
 * Only {@code COMPLETED} transactions are included in all totals and the count.
 * Incoming transfers are counted as deposits; outgoing transfers as withdrawals.
 * Each map key is an ISO 4217 currency code; amounts are never aggregated across currencies.
 *
 * @param accountId           the account this summary covers
 * @param totalDeposits       sum of completed deposits and incoming transfers per currency
 * @param totalWithdrawals    sum of completed withdrawals and outgoing transfers per currency
 * @param transactionCount    total number of completed transactions involving this account
 * @param mostRecentTransaction UTC timestamp of the most recent completed transaction,
 *                            or {@code null} if there are none
 */
public record SummaryResponse(
        String accountId,
        Map<String, BigDecimal> totalDeposits,
        Map<String, BigDecimal> totalWithdrawals,
        long transactionCount,
        Instant mostRecentTransaction
) {}
