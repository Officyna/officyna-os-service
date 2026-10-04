package br.com.officyna.api.vehicle.handler;

import br.com.officyna.domain.vehicle.exception.VehicleBusinessException;
import br.com.officyna.domain.vehicle.exception.VehicleNotFoundException;
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
public class VehicleExceptionHandler {

    @ExceptionHandler(VehicleNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            VehicleNotFoundException ex,
            HttpServletRequest request
    ) {
        String correlationId = resolveCorrelationId(request);
        log.warn("Vehicle not found on [{} {}] - correlationId={}: {}",
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

    @ExceptionHandler(VehicleBusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(
            VehicleBusinessException ex,
            HttpServletRequest request
    ) {
        String correlationId = resolveCorrelationId(request);
        log.warn("Vehicle business error on [{} {}] - correlationId={}: {}",
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
