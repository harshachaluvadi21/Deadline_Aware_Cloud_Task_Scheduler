package web.controller;

import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Centralized exception handler formatting validation and business constraint errors
 * into structured HTTP 400 responses.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult()
                .getAllErrors()
                .stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .toList();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", "Validation Failed");
        body.put("messages", errors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", "Invalid Request");
        body.put("messages", List.of(ex.getMessage()));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalState(IllegalStateException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", "Execution State Error");
        body.put("messages", List.of(ex.getMessage()));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(web.exception.CloudWorkerException.class)
    public ResponseEntity<Map<String, Object>> handleCloudWorkerException(web.exception.CloudWorkerException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", "Worker Unavailable");
        body.put("messages", List.of(ex.getMessage()));

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
    }
}


