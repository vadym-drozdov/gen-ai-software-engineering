package com.homework.banking.controller;

import com.homework.banking.config.RateLimitingFilter;
import com.homework.banking.dto.BalanceResponse;
import com.homework.banking.dto.InterestResponse;
import com.homework.banking.dto.SummaryResponse;
import com.homework.banking.exception.ValidationException;
import com.homework.banking.service.AccountService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        value = AccountController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = RateLimitingFilter.class
        )
)
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AccountService accountService;

    // ── GET /accounts/{id}/balance ─────────────────────────────────────────

    @Test
    void getBalance_returns200WithBalanceBycurrency() throws Exception {
        when(accountService.getBalance("ACC-12345"))
                .thenReturn(new BalanceResponse("ACC-12345", Map.of("USD", new BigDecimal("700.00"))));

        mockMvc.perform(get("/accounts/ACC-12345/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value("ACC-12345"))
                .andExpect(jsonPath("$.balances.USD").value(700.00));
    }

    // ── GET /accounts/{id}/summary ─────────────────────────────────────────

    @Test
    void getSummary_returns200WithTotalsAndCount() throws Exception {
        when(accountService.getSummary("ACC-12345"))
                .thenReturn(new SummaryResponse("ACC-12345",
                        Map.of("USD", new BigDecimal("1000.00")),
                        Map.of("USD", new BigDecimal("300.00")),
                        2L,
                        Instant.parse("2026-04-30T10:00:00Z")));

        mockMvc.perform(get("/accounts/ACC-12345/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value("ACC-12345"))
                .andExpect(jsonPath("$.totalDeposits.USD").value(1000.00))
                .andExpect(jsonPath("$.totalWithdrawals.USD").value(300.00))
                .andExpect(jsonPath("$.transactionCount").value(2));
    }

    // ── GET /accounts/{id}/interest ────────────────────────────────────────

    @Test
    void getInterest_validParams_returns200WithInterestCalculation() throws Exception {
        when(accountService.getInterest("ACC-12345", 0.05, 30))
                .thenReturn(new InterestResponse("ACC-12345",
                        Map.of("USD", new BigDecimal("700.00")),
                        0.05, 30,
                        Map.of("USD", new BigDecimal("2.88")),
                        Map.of("USD", new BigDecimal("702.88"))));

        mockMvc.perform(get("/accounts/ACC-12345/interest?rate=0.05&days=30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value("ACC-12345"))
                .andExpect(jsonPath("$.interest.USD").value(2.88))
                .andExpect(jsonPath("$.projectedBalance.USD").value(702.88));
    }

    @Test
    void getInterest_negativeRate_returns400WithRateField() throws Exception {
        when(accountService.getInterest("ACC-12345", -1.0, 30))
                .thenThrow(new ValidationException("rate", "Rate must be a positive number"));

        mockMvc.perform(get("/accounts/ACC-12345/interest?rate=-1&days=30"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].field").value("rate"));
    }

    @Test
    void getInterest_zeroDays_returns400WithDaysField() throws Exception {
        when(accountService.getInterest("ACC-12345", 0.05, 0))
                .thenThrow(new ValidationException("days", "Days must be at least 1"));

        mockMvc.perform(get("/accounts/ACC-12345/interest?rate=0.05&days=0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].field").value("days"));
    }

    @Test
    void getInterest_nonNumericRate_returns400WithRateField() throws Exception {
        mockMvc.perform(get("/accounts/ACC-12345/interest?rate=bad&days=30"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].field").value("rate"));
    }

    @Test
    void getInterest_missingDays_returns400() throws Exception {
        mockMvc.perform(get("/accounts/ACC-12345/interest?rate=0.05"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].field").value("days"));
    }
}
