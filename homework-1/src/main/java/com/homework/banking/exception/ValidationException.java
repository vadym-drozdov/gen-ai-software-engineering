package com.homework.banking.exception;

/**
 * Signals a business-rule violation in the service layer. Unlike Bean Validation,
 * which catches format errors in the request DTO, this exception is thrown when
 * rules depend on the combination of fields or on application state
 * (e.g. a deposit missing its {@code toAccount}, or a transfer between identical accounts).
 *
 * <p>The {@code field} name is propagated to the HTTP 400 response so clients
 * can identify exactly which field triggered the rejection.</p>
 */
public class ValidationException extends RuntimeException {

    private final String field;

    /**
     * @param field   name of the request field that caused the violation
     * @param message human-readable explanation
     */
    public ValidationException(String field, String message) {
        super(message);
        this.field = field;
    }

    /** Returns the name of the request field that caused the violation. */
    public String getField() {
        return field;
    }
}
