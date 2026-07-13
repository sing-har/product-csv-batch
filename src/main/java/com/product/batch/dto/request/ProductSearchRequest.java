package com.product.batch.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.product.batch.enums.DateFilterType;
import com.product.batch.enums.UpdateStatus;

import java.time.LocalDate;

public record ProductSearchRequest(
        String productId,
        String name,
        String category,

        @JsonFormat(pattern = "yyyy-MM-dd") LocalDate dateFrom,

        @JsonFormat(pattern = "yyyy-MM-dd") LocalDate dateTo,

        DateFilterType dateFilterType,
        UpdateStatus updateStatus) {
}