package br.com.officyna.infrastructure.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
    int status,
    String message,
    String path,
    String correlationId,
    Map<String, String> errors,
    LocalDateTime timestamp
) {
    public static ErrorResponse of(int status, String message) {
        return new ErrorResponse(status, message, null, null, null, LocalDateTime.now());
    }

    public static ErrorResponse of(int status, String message, String path, String correlationId) {
        return new ErrorResponse(status, message, path, correlationId, null, LocalDateTime.now());
    }

    public static ErrorResponse ofValidation(Map<String, String> errors) {
        return new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Validation failed",
                null,
                null,
                errors,
                LocalDateTime.now()
        );
    }

    public static ErrorResponse ofValidation(Map<String, String> errors, String path, String correlationId) {
        return new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Validation failed",
                path,
                correlationId,
                errors,
                LocalDateTime.now()
        );
    }
}

