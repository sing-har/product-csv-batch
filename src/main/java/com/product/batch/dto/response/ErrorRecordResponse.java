package com.product.batch.dto.response;

import java.time.LocalDateTime;

public record ErrorRecordResponse(
        Long id,
        Long jobId,
        String rawData,
        String errorMessage,
        LocalDateTime createdAt) {
}