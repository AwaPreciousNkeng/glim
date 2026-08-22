package com.codewithpcodes.glimserver.handler;

import com.codewithpcodes.glimserver.exceptions.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
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
        return body(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS",e.getMessage());
    }

    @ExceptionHandler(InvalidCodeException.class)
    public ResponseEntity<Map<String, Object>> code(InvalidCodeException e) {
        return body(HttpStatus.BAD_REQUEST, "INVALID_CODE", e.getMessage());
    }

    @ExceptionHandler(TooManyRequestsException.class)
    public ResponseEntity<Map<String, Object>> rate(TooManyRequestsException e) {
        return body(HttpStatus. TOO_MANY_REQUESTS, "RATE_LIMITED", e.getMessage());
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<Map<String, Object>> badRequest(BadRequestException e) {
        return body(HttpStatus.BAD_REQUEST, "BAD_REQUEST", e.getMessage());
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<Map<String, Object>> duplicate(DuplicateResourceException e) {
        return body(HttpStatus.UNAUTHORIZED, "DUPLICATE_RESOURCE", e.getMessage());
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<Map<String, Object>> forbidden(ForbiddenException e) {
        return body(HttpStatus.FORBIDDEN, "FORBIDDEN_REQUEST", e.getMessage());
    }

    @ExceptionHandler(InvalidFileException.class)
    public ResponseEntity<Map<String, Object>> invalidFile(InvalidFileException e) {
        return body(HttpStatus.BAD_REQUEST, "INVALID_FILE", e.getMessage());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> resource(ResourceNotFoundException e) {
        return body(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", e.getMessage());
    }

    @ExceptionHandler(SmsDeliveryException.class)
    public ResponseEntity<Map<String, Object>> sms(SmsDeliveryException e) {
        return body(HttpStatus.BAD_REQUEST, "SMS_DELIVERY", e.getMessage());
    }

    @ExceptionHandler(StorageException.class)
    public ResponseEntity<Map<String, Object>> storage(StorageException e) {
        return body(HttpStatus.BAD_REQUEST, "STORAGE_EXCEPTION", e.getMessage());
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Map<String, Object>> unAuthorized(UnauthorizedException e) {
        return body(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> generic(Exception e) {
        log.error("Unhandled Exception ", e);
        return body(HttpStatus.INTERNAL_SERVER_ERROR, "UNHANDLED_ERROR", "An unexpected error");
    }
}
