package com.homework.banking.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

/**
 * Request body for creating a transaction. Bean Validation enforces format constraints;
 * business rules (e.g. which account fields are required for each type) are enforced
 * by {@link com.homework.banking.service.TransactionService}.
 *
 * <ul>
 *   <li>{@code deposit} — requires {@code toAccount}; {@code fromAccount} is ignored</li>
 *   <li>{@code withdrawal} — requires {@code fromAccount}; {@code toAccount} is ignored</li>
 *   <li>{@code transfer} — requires both accounts, and they must differ</li>
 * </ul>
 */
public class CreateTransactionRequest {

    /** Positive monetary amount, at most 2 decimal places (e.g. {@code 100.00}). */
    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be a positive number")
    @Digits(integer = 15, fraction = 2, message = "Amount must have at most 2 decimal places")
    private BigDecimal amount;

    /** Source account in {@code ACC-XXXXX} format (5 alphanumeric chars); nullable for deposits. */
    @Pattern(regexp = "ACC-[A-Za-z0-9]{5}", message = "Account must match format ACC-XXXXX (5 alphanumeric chars)")
    private String fromAccount;

    /** Destination account in {@code ACC-XXXXX} format; nullable for withdrawals. */
    @Pattern(regexp = "ACC-[A-Za-z0-9]{5}", message = "Account must match format ACC-XXXXX (5 alphanumeric chars)")
    private String toAccount;

    /** ISO 4217 currency code. Accepted values: {@code USD, EUR, GBP, JPY, CHF, CAD, AUD}. */
    @NotBlank(message = "Currency is required")
    @Pattern(regexp = "USD|EUR|GBP|JPY|CHF|CAD|AUD", message = "Currency must be one of: USD, EUR, GBP, JPY, CHF, CAD, AUD")
    private String currency;

    /** Transaction type. Accepted values: {@code deposit, withdrawal, transfer}. */
    @NotBlank(message = "Type is required")
    @Pattern(regexp = "deposit|withdrawal|transfer", message = "Type must be one of: deposit, withdrawal, transfer")
    private String type;

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getFromAccount() { return fromAccount; }
    public void setFromAccount(String fromAccount) { this.fromAccount = fromAccount; }

    public String getToAccount() { return toAccount; }
    public void setToAccount(String toAccount) { this.toAccount = toAccount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
}
