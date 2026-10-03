package br.com.officyna.api.customer.handler;

import br.com.officyna.domain.customer.exception.CustomerBusinessException;
import br.com.officyna.domain.customer.exception.CustomerNotFoundException;
import br.com.officyna.infrastructure.exception.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class CustomerExceptionHandler {

    @ExceptionHandler(CustomerNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            CustomerNotFoundException ex,
            HttpServletRequest request
    ) {
        String correlationId = resolveCorrelationId(request);
        log.warn("Customer not found on [{} {}] - correlationId={}: {}",
                request.getMethod(), request.getRequestURI(), correlationId, ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(
                        HttpStatus.NOT_FOUND.value(),
                        ex.getMessage(),
                        request.getRequestURI(),
                        correlationId
                ));
    }

    @ExceptionHandler(CustomerBusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(
            CustomerBusinessException ex,
            HttpServletRequest request
    ) {
        String correlationId = resolveCorrelationId(request);
        log.warn("Customer business error on [{} {}] - correlationId={}: {}",
                request.getMethod(), request.getRequestURI(), correlationId, ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(
                        HttpStatus.BAD_REQUEST.value(),
                        ex.getMessage(),
                        request.getRequestURI(),
                        correlationId
                ));
    }

    private String resolveCorrelationId(HttpServletRequest request) {
        String correlationId = MDC.get("correlationId");
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = request.getHeader("X-Correlation-ID");
        }
        return correlationId != null ? correlationId : "unknown";
    }
}
