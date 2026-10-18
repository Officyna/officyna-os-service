package br.com.officyna.infrastructure.client.production;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record LaborClientResponse(
        String id,
        String name,
        String description,
        BigDecimal price,
        Integer executionTimeInDays,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Boolean active
) {}
