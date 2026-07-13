package com.product.batch.service;

import com.product.batch.dto.request.ProductSearchRequest;
import com.product.batch.dto.response.ProductResponse;
import com.product.batch.entity.Product;
import com.product.batch.enums.DateFilterType;
import com.product.batch.enums.UpdateStatus;
import com.product.batch.repository.ProductRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    public List<ProductResponse> searchProducts(ProductSearchRequest request) {
        ProductSearchRequest safeRequest = request == null
                ? new ProductSearchRequest(null, null, null, null, null, null, null)
                : request;

        validateSearchRequest(safeRequest);

        Specification<Product> specification = buildProductSearchSpecification(safeRequest);

        return productRepository.findAll(specification, Sort.by(Sort.Direction.ASC, "id"))
                .stream()
                .map(this::toProductResponse)
                .toList();
    }

    private void validateSearchRequest(ProductSearchRequest request) {
        LocalDate dateFrom = request.dateFrom();
        LocalDate dateTo = request.dateTo();

        boolean hasDateRange = dateFrom != null || dateTo != null;

        if (dateFrom != null && dateTo != null && dateFrom.isAfter(dateTo)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "dateFrom cannot be greater than dateTo");
        }

        if (!hasDateRange && request.dateFilterType() != null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "dateFilterType requires dateFrom or dateTo");
        }

        UpdateStatus updateStatus = request.updateStatus() == null
                ? UpdateStatus.ALL
                : request.updateStatus();

        if (request.dateFilterType() == DateFilterType.UPDATED_DATE
                && updateStatus == UpdateStatus.NOT_UPDATED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "UPDATED_DATE filter cannot be used with NOT_UPDATED status");
        }
    }

    private Specification<Product> buildProductSearchSpecification(ProductSearchRequest request) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (hasText(request.productId())) {
                predicates.add(
                        criteriaBuilder.equal(
                                criteriaBuilder.lower(root.get("productId")),
                                request.productId().trim().toLowerCase()));
            }

            if (hasText(request.name())) {
                predicates.add(
                        criteriaBuilder.like(
                                criteriaBuilder.lower(root.get("name")),
                                "%" + request.name().trim().toLowerCase() + "%"));
            }

            if (hasText(request.category())) {
                predicates.add(
                        criteriaBuilder.equal(
                                criteriaBuilder.lower(root.get("category")),
                                request.category().trim().toLowerCase()));
            }

            UpdateStatus updateStatus = request.updateStatus() == null
                    ? UpdateStatus.ALL
                    : request.updateStatus();

            if (updateStatus == UpdateStatus.UPDATED) {
                predicates.add(criteriaBuilder.isNotNull(root.get("updatedDate")));
            }

            if (updateStatus == UpdateStatus.NOT_UPDATED) {
                predicates.add(criteriaBuilder.isNull(root.get("updatedDate")));
            }

            addDateFilters(request, predicates, root, criteriaBuilder);

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private void addDateFilters(
            ProductSearchRequest request,
            List<Predicate> predicates,
            jakarta.persistence.criteria.Root<Product> root,
            jakarta.persistence.criteria.CriteriaBuilder criteriaBuilder) {
        LocalDate dateFrom = request.dateFrom();
        LocalDate dateTo = request.dateTo();

        boolean hasDateRange = dateFrom != null || dateTo != null;

        if (!hasDateRange) {
            return;
        }

        DateFilterType dateFilterType = request.dateFilterType() == null
                ? DateFilterType.CREATED_DATE
                : request.dateFilterType();

        if (dateFilterType == DateFilterType.CREATED_DATE) {
            if (dateFrom != null) {
                predicates.add(
                        criteriaBuilder.greaterThanOrEqualTo(root.get("createdDate"), dateFrom));
            }

            if (dateTo != null) {
                predicates.add(
                        criteriaBuilder.lessThanOrEqualTo(root.get("createdDate"), dateTo));
            }
        }

        if (dateFilterType == DateFilterType.UPDATED_DATE) {
            if (dateFrom != null) {
                predicates.add(
                        criteriaBuilder.greaterThanOrEqualTo(
                                root.get("updatedDate"),
                                dateFrom.atStartOfDay()));
            }

            if (dateTo != null) {
                predicates.add(
                        criteriaBuilder.lessThanOrEqualTo(
                                root.get("updatedDate"),
                                dateTo.atTime(LocalTime.MAX)));
            }
        }
    }

    private ProductResponse toProductResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getProductId(),
                product.getName(),
                product.getCategory(),
                product.getPrice(),
                product.getQuantity(),
                product.getTotalValue(),
                product.getCreatedDate(),
                product.getUpdatedDate());
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}