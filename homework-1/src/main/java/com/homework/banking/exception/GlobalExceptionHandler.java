package com.homework.banking.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;

/**
 * Centralizes exception-to-HTTP-response mapping for all controllers.
 * Every handled exception produces a consistent {@link ErrorResponse} JSON body
 * with an {@code error} summary and a {@code details} list that names the offending field.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles Bean Validation failures on {@code @RequestBody} or {@code @ModelAttribute}.
     * Each violated constraint becomes its own entry in {@code details}.
     *
     * @return 400 Bad Request with one {@link ErrorResponse.FieldError} per constraint violation
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        List<ErrorResponse.FieldError> details = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ErrorResponse.FieldError(fe.getField(), fe.getDefaultMessage()))
                .toList();
        return ResponseEntity.badRequest().body(ErrorResponse.validationFailed(details));
    }

    /**
     * Handles business-rule violations raised by the service layer.
     * The {@link ValidationException#getField()} value is forwarded as the error field.
     *
     * @return 400 Bad Request identifying the offending field
     */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(ValidationException ex) {
        return ResponseEntity.badRequest()
                .body(ErrorResponse.validationFailed(ex.getField(), ex.getMessage()));
    }

    /**
     * Handles missing transaction lookups.
     *
     * @return 404 Not Found with {@code field = "id"} in the error detail
     */
    @ExceptionHandler(TransactionNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(TransactionNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                new ErrorResponse("Not Found", List.of(new ErrorResponse.FieldError("id", ex.getMessage())))
        );
    }

    /**
     * Handles malformed or missing JSON request bodies.
     *
     * @return 400 Bad Request with {@code field = "body"}
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest()
                .body(ErrorResponse.validationFailed("body", "Malformed or missing JSON body"));
    }

    /**
     * Handles missing required {@code @RequestParam} values.
     * The parameter name is used as the error field.
     *
     * @return 400 Bad Request naming the missing parameter
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(MissingServletRequestParameterException ex) {
        String message = "Required parameter '" + ex.getParameterName() + "' is missing";
        return ResponseEntity.badRequest()
                .body(ErrorResponse.validationFailed(ex.getParameterName(), message));
    }

    /**
     * Handles type conversion failures for {@code @RequestParam} values
     * (e.g. a non-numeric string where a number is expected).
     *
     * @return 400 Bad Request naming the mismatched parameter
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String message = "Invalid value '" + ex.getValue() + "' for parameter '" + ex.getName() + "'";
        return ResponseEntity.badRequest()
                .body(ErrorResponse.validationFailed(ex.getName(), message));
    }

    /**
     * Fallback handler for unrecognized {@code IllegalArgumentException}s
     * (e.g. unknown enum values passed to service methods).
     *
     * @return 400 Bad Request with {@code field = "request"}
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest()
                .body(new ErrorResponse("Bad Request",
                        List.of(new ErrorResponse.FieldError("request", ex.getMessage()))));
    }
}
