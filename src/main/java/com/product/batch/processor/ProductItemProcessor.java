package com.product.batch.processor;

import com.product.batch.entity.ErrorRecord;
import com.product.batch.entity.Product;
import com.product.batch.model.ProductCsvRow;
import com.product.batch.repository.ErrorRecordRepository;
import com.product.batch.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@StepScope
@RequiredArgsConstructor
@Slf4j
public class ProductItemProcessor implements ItemProcessor<ProductCsvRow, Product> {

    private final ProductRepository productRepository;
    private final ErrorRecordRepository errorRecordRepository;

    private static final DateTimeFormatter ISO_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final DateTimeFormatter INDIAN_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");

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

        String normalizedProductId = productId.toUpperCase(Locale.ROOT);

        if (!productIdsInCurrentJob.add(normalizedProductId)) {
            throw new IllegalArgumentException("Duplicate product_id found in CSV file: " + productId);
        }

        BigDecimal price = parsePrice(row.getPrice());
        Integer quantity = parseQuantity(row.getQuantity());
        LocalDate createdDate = parseCreatedDate(row.getCreatedDate());

        BigDecimal totalValue = price.multiply(BigDecimal.valueOf(quantity));

        return productRepository.findByProductIdIgnoreCase(productId)
                .map(existingProduct -> updateExistingProduct(
                        existingProduct,
                        category,
                        price,
                        quantity,
                        totalValue))
                .orElseGet(() -> createNewProduct(
                        productId,
                        name,
                        category,
                        price,
                        quantity,
                        totalValue,
                        createdDate));
    }

    private Product createNewProduct(
            String productId,
            String name,
            String category,
            BigDecimal price,
            Integer quantity,
            BigDecimal totalValue,
            LocalDate createdDate) {
        log.debug("Creating new product. productId={}", productId);

        return Product.builder()
                .productId(productId)
                .name(name)
                .category(category.toUpperCase(Locale.ROOT))
                .price(price)
                .quantity(quantity)
                .totalValue(totalValue)
                .createdDate(createdDate)
                .updatedDate(null)
                .build();
    }

    private Product updateExistingProduct(
            Product existingProduct,
            String category,
            BigDecimal price,
            Integer quantity,
            BigDecimal totalValue) {
        log.debug("Updating existing product. productId={}", existingProduct.getProductId());

        existingProduct.setCategory(category.toUpperCase(Locale.ROOT));
        existingProduct.setPrice(price);
        existingProduct.setQuantity(quantity);
        existingProduct.setTotalValue(totalValue);
        existingProduct.setUpdatedDate(LocalDateTime.now());

        return existingProduct;
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
        String dateValue = required(value, "created_date is mandatory");

        try {
            return LocalDate.parse(dateValue, ISO_DATE_FORMATTER);
        } catch (Exception ignored) {
            // Skipping: Try next format
        }

        try {
            return LocalDate.parse(dateValue, INDIAN_DATE_FORMATTER);
        } catch (Exception ignored) {
            throw new IllegalArgumentException(
                    "created_date must be a valid date in yyyy-MM-dd or dd-MM-yyyy format");
        }
    }

    private void saveErrorRecord(ProductCsvRow row, String errorMessage) {
        ErrorRecord errorRecord = ErrorRecord.builder()
                .jobId(jobId)
                .rawData(row.toRawData())
                .errorMessage(errorMessage)
                .createdAt(LocalDateTime.now())
                .build();

        errorRecordRepository.save(errorRecord);

        log.debug(
                "Invalid product row captured. jobId={}, rawData={}, error={}",
                jobId,
                row.toRawData(),
                errorMessage);
    }
}