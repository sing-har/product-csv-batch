package com.product.batch.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ProductResponse(
        Long id,
        String productId,
        String name,
        String category,
        BigDecimal price,
        Integer quantity,
        BigDecimal totalValue,
        LocalDate createdDate,
        LocalDateTime updatedDate) {
}