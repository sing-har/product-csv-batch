package com.product.batch.dto.response;

public record BatchJobResponse(
        Long jobExecutionId,
        String jobStatus,
        String originalFileName) {
}