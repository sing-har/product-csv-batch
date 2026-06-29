package com.product.batch.processor;

import com.product.batch.entity.ErrorRecord;
import com.product.batch.entity.Product;
import com.product.batch.model.ProductCsvRow;
import com.product.batch.repository.ErrorRecordRepository;
import com.product.batch.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import java.util.Objects;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@StepScope
@RequiredArgsConstructor
public class ProductItemProcessor implements ItemProcessor<ProductCsvRow, Product> {

    private final ProductRepository productRepository;
    private final ErrorRecordRepository errorRecordRepository;

    private final Set<String> productIdsInCurrentJob = ConcurrentHashMap.newKeySet();

    @Value("#{stepExecution.jobExecution.id}")
    private Long jobId;

    @Override
    public @Nullable Product process(@NonNull ProductCsvRow row) {
        try {
            return validateAndTransform(row);
        } catch (Exception exception) {
            saveErrorRecord(row, exception.getMessage());
            return null;
        }
    }

    private Product validateAndTransform(ProductCsvRow row) {
        String productId = required(row.getProductId(), "product_id is mandatory");
        String name = required(row.getName(), "name is mandatory");
        String category = required(row.getCategory(), "category is mandatory");

        if (!productIdsInCurrentJob.add(productId)) {
            throw new IllegalArgumentException("Duplicate product_id found in CSV file: " + productId);
        }

        if (productRepository.existsByProductId(productId)) {
            throw new IllegalArgumentException("product_id already exists in database: " + productId);
        }

        BigDecimal price = parsePrice(row.getPrice());
        Integer quantity = parseQuantity(row.getQuantity());
        LocalDate createdDate = parseCreatedDate(row.getCreatedDate());

        BigDecimal totalValue = price.multiply(BigDecimal.valueOf(quantity));

        return Product.builder()
                .productId(productId)
                .name(name)
                .category(category.toUpperCase())
                .price(price)
                .quantity(quantity)
                .totalValue(totalValue)
                .createdDate(createdDate)
                .build();
    }

    private String required(String value, String errorMessage) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(errorMessage);
        }
        return value.trim();
    }

    private BigDecimal parsePrice(String value) {
        try {
            BigDecimal price = new BigDecimal(required(value, "price is mandatory"));

            if (price.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("price must be greater than 0");
            }

            return price;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("price must be a valid number");
        }
    }

    private Integer parseQuantity(String value) {
        try {
            Integer quantity = Integer.valueOf(required(value, "quantity is mandatory"));

            if (quantity < 0) {
                throw new IllegalArgumentException("quantity must be greater than or equal to 0");
            }

            return quantity;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("quantity must be a valid integer");
        }
    }

    private LocalDate parseCreatedDate(String value) {
        try {
            return LocalDate.parse(required(value, "created_date is mandatory"));
        } catch (Exception exception) {
            throw new IllegalArgumentException("created_date must be a valid date in yyyy-MM-dd format");
        }
    }

    private void saveErrorRecord(ProductCsvRow row, String errorMessage) {
        ErrorRecord errorRecord = ErrorRecord.builder()
                .jobId(jobId)
                .rawData(row.toRawData())
                .errorMessage(errorMessage)
                .createdAt(LocalDateTime.now())
                .build();

        errorRecordRepository.save(Objects.requireNonNull(errorRecord));
    }
}