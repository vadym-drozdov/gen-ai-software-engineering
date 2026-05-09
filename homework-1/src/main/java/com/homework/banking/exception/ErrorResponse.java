package com.homework.banking.exception;

import java.util.List;

/**
 * Standard error envelope returned by all 4xx and 5xx responses.
 * The {@code details} list always contains at least one entry that names
 * the offending field, making it straightforward for clients to highlight
 * the exact form field that caused the error.
 *
 * @param error   short human-readable error category (e.g. {@code "Validation failed"})
 * @param details one entry per violated constraint, each naming the offending field
 */
public record ErrorResponse(String error, List<FieldError> details) {

    /**
     * A single constraint violation.
     *
     * @param field   name of the offending request field (e.g. {@code "amount"}, {@code "currency"})
     * @param message human-readable explanation of the violation
     */
    public record FieldError(String field, String message) {}

    /**
     * Convenience factory for a single-field validation failure.
     *
     * @param field   name of the offending field
     * @param message violation message
     * @return an {@code ErrorResponse} with {@code error = "Validation failed"}
     */
    public static ErrorResponse validationFailed(String field, String message) {
        return new ErrorResponse("Validation failed", List.of(new FieldError(field, message)));
    }

    /**
     * Convenience factory for multi-field validation failures (e.g. from Bean Validation).
     *
     * @param details pre-built list of field errors
     * @return an {@code ErrorResponse} with {@code error = "Validation failed"}
     */
    public static ErrorResponse validationFailed(List<FieldError> details) {
        return new ErrorResponse("Validation failed", details);
    }
}
