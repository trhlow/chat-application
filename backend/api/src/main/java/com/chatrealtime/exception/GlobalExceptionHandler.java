package com.chatrealtime.exception;

import com.chatrealtime.dto.response.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ApplicationException.class)
    public ResponseEntity<ApiErrorResponse> handleApplicationException(
            ApplicationException exception,
            HttpServletRequest request
    ) {
        return ResponseEntity
                .status(exception.getStatus())
                .body(buildError(
                        exception.getStatus().value(),
                        exception.getStatus().name(),
                        exception.getMessage(),
                        request.getRequestURI(),
                        null
                ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(fieldError ->
                fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage() == null
                        ? "Invalid value"
                        : fieldError.getDefaultMessage())
        );
        String message = fieldErrors.values().stream().findFirst().orElse("Invalid request body");
        return ResponseEntity.badRequest().body(
                buildError(400, "BAD_REQUEST", message, request.getRequestURI(), fieldErrors)
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException exception,
            HttpServletRequest request
    ) {
        return ResponseEntity.badRequest()
                .body(buildError(400, "BAD_REQUEST", exception.getMessage(), request.getRequestURI(), null));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthenticationException(
            AuthenticationException exception,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(401)
                .body(buildError(401, "UNAUTHORIZED", exception.getMessage(), request.getRequestURI(), null));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDeniedException(
            AccessDeniedException exception,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(403)
                .body(buildError(403, "FORBIDDEN", exception.getMessage(), request.getRequestURI(), null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleException(Exception exception, HttpServletRequest request) {
        log.error("Unhandled error {} {}", request.getMethod(), request.getRequestURI(), exception);
        return ResponseEntity.status(500)
                .body(buildError(500, "INTERNAL_SERVER_ERROR", "Internal server error", request.getRequestURI(), null));
    }

    private ApiErrorResponse buildError(
            int status,
            String error,
            String message,
            String path,
            Map<String, String> fieldErrors
    ) {
        return new ApiErrorResponse(Instant.now(), status, error, message, path, fieldErrors);
    }
}

