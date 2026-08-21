package com.codewithpcodes.glimserver.handler;

import com.codewithpcodes.glimserver.exceptions.InvalidCodeException;
import com.codewithpcodes.glimserver.exceptions.InvalidCredentialsException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private ResponseEntity<Map<String, Object>> body(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(new LinkedHashMap<>(Map.of(
                "timestamp", Instant.now().toString(),
                "code", code,
                "message", message
        )));
    }
    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<Map<String, Object>> credentials(InvalidCredentialsException e) {
        return body(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS","Incorrect email/phone or password.");
    }

    @ExceptionHandler(InvalidCodeException.class)
    public ResponseEntity<Map<String, Object>> code(InvalidCodeException e) {

    }
}
