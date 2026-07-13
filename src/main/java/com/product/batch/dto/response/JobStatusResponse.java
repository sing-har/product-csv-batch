package com.product.batch.dto.response;

import java.time.LocalDateTime;

public record JobStatusResponse(
        Long jobExecutionId,
        String jobName,
        String status,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String exitCode,
        String exitMessage) {
}