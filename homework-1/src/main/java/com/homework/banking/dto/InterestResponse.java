package com.homework.banking.dto;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Simple interest projection for an account over a given period.
 * Interest is computed per currency as: {@code balance × rate × days / 365},
 * rounded half-up to 2 decimal places.
 *
 * @param accountId       the account this projection covers
 * @param balances        current net balance per currency code
 * @param rate            annual interest rate as a decimal (e.g. {@code 0.05} for 5%)
 * @param days            number of days over which interest accrues
 * @param interest        computed interest amount per currency
 * @param projectedBalance balance plus interest per currency ({@code balances + interest})
 */
public record InterestResponse(
        String accountId,
        Map<String, BigDecimal> balances,
        double rate,
        int days,
        Map<String, BigDecimal> interest,
        Map<String, BigDecimal> projectedBalance
) {}
