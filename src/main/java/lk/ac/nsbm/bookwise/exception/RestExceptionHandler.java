package lk.ac.nsbm.bookwise.exception;

import lk.ac.nsbm.bookwise.controller.BookRestController;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * The REST half of the centralised error handling.
 *
 * The same BookWiseException hierarchy serves both front ends: the Thymeleaf
 * screens render it as a page, and this advice renders it as JSON with the
 * status code the exception itself nominates. Neither controller contains a
 * try/catch.
 */
@RestControllerAdvice(assignableTypes = BookRestController.class)
public class RestExceptionHandler {

    @ExceptionHandler(BookWiseException.class)
    public ResponseEntity<Map<String, Object>> handleBusinessFailure(BookWiseException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", ex.getHttpStatus().value());
        body.put("errorCode", ex.getErrorCode());
        body.put("title", ex.getTitle());
        body.put("message", ex.getUserMessage());
        body.put("action", ex.getSuggestedAction());
        return ResponseEntity.status(ex.getHttpStatus()).body(body);
    }

    /** Bean Validation failures on @RequestBody, reported field by field. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new TreeMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> fieldErrors.put(error.getField(), error.getDefaultMessage()));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", 400);
        body.put("errorCode", "VALIDATION_FAILED");
        body.put("title", "Invalid book details");
        body.put("fieldErrors", fieldErrors);
        return ResponseEntity.badRequest().body(body);
    }
}
