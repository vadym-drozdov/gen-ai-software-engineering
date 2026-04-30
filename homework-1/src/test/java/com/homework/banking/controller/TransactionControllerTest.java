package com.homework.banking.controller;

import com.homework.banking.config.RateLimitingFilter;
import com.homework.banking.exception.TransactionNotFoundException;
import com.homework.banking.exception.ValidationException;
import com.homework.banking.model.Transaction;
import com.homework.banking.model.TransactionStatus;
import com.homework.banking.model.TransactionType;
import com.homework.banking.service.TransactionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        value = TransactionController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = RateLimitingFilter.class
        )
)
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TransactionService transactionService;

    // ── POST /transactions ──────────────────────────────────────────────────

    @Test
    void createDeposit_returns201WithTransactionBody() throws Exception {
        Transaction tx = deposit("ACC-12345", "1000.00");
        when(transactionService.createTransaction(any())).thenReturn(tx);

        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"deposit","toAccount":"ACC-12345","amount":1000.00,"currency":"USD"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(tx.getId()))
                .andExpect(jsonPath("$.type").value("deposit"))
                .andExpect(jsonPath("$.status").value("completed"));
    }

    @Test
    void createTransaction_negativeAmount_returns400WithAmountField() throws Exception {
        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"deposit","toAccount":"ACC-12345","amount":-50.00,"currency":"USD"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation failed"))
                .andExpect(jsonPath("$.details[0].field").value("amount"));
    }

    @Test
    void createTransaction_tooManyDecimalPlaces_returns400() throws Exception {
        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"deposit","toAccount":"ACC-12345","amount":10.123,"currency":"USD"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation failed"))
                .andExpect(jsonPath("$.details[0].field").value("amount"));
    }

    @Test
    void createTransaction_invalidAccountFormat_returns400WithAccountField() throws Exception {
        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"deposit","toAccount":"INVALID","amount":100.00,"currency":"USD"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation failed"))
                .andExpect(jsonPath("$.details[0].field").value("toAccount"));
    }

    @Test
    void createTransaction_invalidCurrency_returns400WithCurrencyField() throws Exception {
        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"deposit","toAccount":"ACC-12345","amount":100.00,"currency":"XYZ"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].field").value("currency"));
    }

    @Test
    void createTransaction_missingType_returns400() throws Exception {
        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"toAccount":"ACC-12345","amount":100.00,"currency":"USD"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation failed"));
    }

    @Test
    void createTransaction_businessRuleViolation_returns400WithRealFieldName() throws Exception {
        when(transactionService.createTransaction(any()))
                .thenThrow(new ValidationException("toAccount", "toAccount is required for deposit transactions"));

        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"deposit","amount":100.00,"currency":"USD"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].field").value("toAccount"));
    }

    @Test
    void createTransaction_malformedJson_returns400WithBodyField() throws Exception {
        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("not-json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].field").value("body"));
    }

    // ── GET /transactions ──────────────────────────────────────────────────

    @Test
    void getAll_noFilters_returns200WithList() throws Exception {
        when(transactionService.getAllTransactions(isNull(), isNull(), isNull(), isNull()))
                .thenReturn(List.of(deposit("ACC-12345", "1000.00")));

        mockMvc.perform(get("/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getAll_withAccountIdFilter_passesFilterToService() throws Exception {
        when(transactionService.getAllTransactions(eq("ACC-12345"), isNull(), isNull(), isNull()))
                .thenReturn(List.of(deposit("ACC-12345", "500.00")));

        mockMvc.perform(get("/transactions?accountId=ACC-12345"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getAll_badDateFormat_returns400WithFromField() throws Exception {
        mockMvc.perform(get("/transactions?from=not-a-date"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].field").value("from"));
    }

    @Test
    void getAll_invalidType_returns400WithTypeField() throws Exception {
        when(transactionService.getAllTransactions(isNull(), eq("bogus"), isNull(), isNull()))
                .thenThrow(new ValidationException("type", "Type must be one of: deposit, withdrawal, transfer"));

        mockMvc.perform(get("/transactions?type=bogus"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].field").value("type"));
    }

    // ── GET /transactions/{id} ─────────────────────────────────────────────

    @Test
    void getById_found_returns200() throws Exception {
        Transaction tx = deposit("ACC-12345", "1000.00");
        when(transactionService.getById(tx.getId())).thenReturn(tx);

        mockMvc.perform(get("/transactions/" + tx.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(tx.getId()));
    }

    @Test
    void getById_notFound_returns404WithIdField() throws Exception {
        when(transactionService.getById("missing"))
                .thenThrow(new TransactionNotFoundException("missing"));

        mockMvc.perform(get("/transactions/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.details[0].field").value("id"));
    }

    // ── GET /transactions/export ───────────────────────────────────────────

    @Test
    void export_csvFormat_returns200WithCsvContentType() throws Exception {
        when(transactionService.exportCsv("csv")).thenReturn("id,fromAccount,...\n");

        mockMvc.perform(get("/transactions/export?format=csv"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("text/csv")));
    }

    @Test
    void export_invalidFormat_returns400WithFormatField() throws Exception {
        when(transactionService.exportCsv("xml"))
                .thenThrow(new ValidationException("format", "Only 'csv' format is supported"));

        mockMvc.perform(get("/transactions/export?format=xml"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].field").value("format"));
    }

    @Test
    void export_missingFormatParam_returns400WithMissingMessage() throws Exception {
        mockMvc.perform(get("/transactions/export"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].field").value("format"))
                .andExpect(jsonPath("$.details[0].message").value(containsString("missing")));
    }

    // ── helpers ────────────────────────────────────────────────────────────

    private Transaction deposit(String toAccount, String amount) {
        return new Transaction(UUID.randomUUID().toString(), null, toAccount,
                new BigDecimal(amount), "USD", TransactionType.DEPOSIT,
                Instant.now(), TransactionStatus.COMPLETED);
    }
}
