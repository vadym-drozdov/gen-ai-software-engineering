package com.homework.banking.dto;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Current balance of an account, grouped by currency.
 *
 * @param accountId the account this balance belongs to
 * @param balances  net balance per currency code (e.g. {@code {"USD": 500.00, "EUR": 200.00}});
 *                  only currencies with completed transactions appear; may be empty
 */
public record BalanceResponse(String accountId, Map<String, BigDecimal> balances) {}
