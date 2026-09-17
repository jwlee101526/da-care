package com.dacare.server.api;
import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(IllegalArgumentException.class) ResponseEntity<Map<String, Object>> illegal(IllegalArgumentException e) { return ResponseEntity.badRequest().body(Map.of("code", "INVALID_REQUEST", "message", e.getMessage())); }
    @ExceptionHandler(NoSuchElementException.class) ResponseEntity<Map<String, Object>> notFound(NoSuchElementException e) { return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("code", "NOT_FOUND", "message", e.getMessage())); }
    @ExceptionHandler(MethodArgumentNotValidException.class) ResponseEntity<Map<String, Object>> validation(MethodArgumentNotValidException e) { Map<String, String> fields = new LinkedHashMap<>(); e.getBindingResult().getFieldErrors().forEach(error -> fields.put(error.getField(), error.getDefaultMessage())); return ResponseEntity.badRequest().body(Map.of("code", "VALIDATION_ERROR", "message", "입력값을 확인하세요.", "fields", fields)); }
}
