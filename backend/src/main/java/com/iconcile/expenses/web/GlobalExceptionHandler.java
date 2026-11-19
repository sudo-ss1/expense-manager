package com.iconcile.expenses.web;

import com.iconcile.expenses.domain.CsvExpenseParser;
import com.iconcile.expenses.service.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.Instant;
import java.util.List;

/** Every error leaves the API in one shape: { status, error, details[], timestamp }. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    public record ApiError(int status, String error, List<String> details, Instant timestamp) {
        static ResponseEntity<ApiError> of(HttpStatus status, String error, List<String> details) {
            return ResponseEntity.status(status).body(new ApiError(status.value(), error, details, Instant.now()));
        }
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> validation(MethodArgumentNotValidException ex) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .toList();
        return ApiError.of(HttpStatus.BAD_REQUEST, "Validation failed", details);
    }

    @ExceptionHandler({IllegalArgumentException.class, CsvExpenseParser.CsvFormatException.class})
    public ResponseEntity<ApiError> badRequest(RuntimeException ex) {
        return ApiError.of(HttpStatus.BAD_REQUEST, ex.getMessage(), List.of());
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiError> unreadable(Exception ex) {
        return ApiError.of(HttpStatus.BAD_REQUEST, "Malformed request - check field formats (date: YYYY-MM-DD, month: YYYY-MM)",
                List.of());
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> notFound(NotFoundException ex) {
        return ApiError.of(HttpStatus.NOT_FOUND, ex.getMessage(), List.of());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiError> tooLarge(MaxUploadSizeExceededException ex) {
        return ApiError.of(HttpStatus.PAYLOAD_TOO_LARGE, "File is too large (max 2 MB)", List.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> conflict(DataIntegrityViolationException ex) {
        return ApiError.of(HttpStatus.CONFLICT, "Request conflicts with existing data", List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> unexpected(Exception ex) {
        if (ex instanceof ErrorResponse framework) {
            // Spring MVC's own exceptions (404 no handler, 405 wrong method, 415 media type, ...) keep their status
            HttpStatus status = HttpStatus.valueOf(framework.getStatusCode().value());
            return ApiError.of(status, status.getReasonPhrase(), List.of());
        }
        log.error("Unhandled error", ex);
        return ApiError.of(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected server error", List.of());
    }
}
