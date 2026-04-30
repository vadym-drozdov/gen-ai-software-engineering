package com.homework.banking.controller;

import com.homework.banking.dto.BalanceResponse;
import com.homework.banking.dto.InterestResponse;
import com.homework.banking.dto.SummaryResponse;
import com.homework.banking.service.AccountService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for account-level analytics under {@code /accounts/{accountId}}.
 * Provides balance, activity summary, and interest projection endpoints.
 * No account creation or deletion — accounts are implied by their transaction history.
 */
@RestController
@RequestMapping("/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    /**
     * Returns the current net balance of an account, grouped by currency.
     * Only {@code COMPLETED} transactions are reflected.
     *
     * @param accountId the account to query
     * @return 200 OK with balance per currency; balances map is empty if no completed transactions exist
     */
    @GetMapping("/{accountId}/balance")
    public ResponseEntity<BalanceResponse> getBalance(@PathVariable String accountId) {
        return ResponseEntity.ok(accountService.getBalance(accountId));
    }

    /**
     * Returns aggregated deposit totals, withdrawal totals, transaction count,
     * and most recent transaction timestamp for an account.
     * Incoming transfers count as deposits; outgoing transfers count as withdrawals.
     *
     * @param accountId the account to summarize
     * @return 200 OK with the account summary
     */
    @GetMapping("/{accountId}/summary")
    public ResponseEntity<SummaryResponse> getSummary(@PathVariable String accountId) {
        return ResponseEntity.ok(accountService.getSummary(accountId));
    }

    /**
     * Calculates a simple-interest projection for an account.
     * Formula: {@code interest = balance × rate × days / 365} per currency,
     * rounded half-up to 2 decimal places.
     *
     * @param accountId the account to project
     * @param rate      annual interest rate as a decimal (e.g. {@code 0.05}); must be positive
     * @param days      number of days to accrue (must be ≥ 1)
     * @return 200 OK with current balances, interest, and projected balances per currency;
     *         400 Bad Request if {@code rate} is not positive or {@code days} is less than 1
     */
    @GetMapping("/{accountId}/interest")
    public ResponseEntity<InterestResponse> getInterest(
            @PathVariable String accountId,
            @RequestParam double rate,
            @RequestParam int days) {
        return ResponseEntity.ok(accountService.getInterest(accountId, rate, days));
    }
}
