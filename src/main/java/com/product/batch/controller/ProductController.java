package com.product.batch.controller;

import com.product.batch.dto.request.ProductSearchRequest;
import com.product.batch.dto.response.ApiResponse;
import com.product.batch.dto.response.ProductResponse;
import com.product.batch.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Slf4j
public class ProductController {

    private final ProductService productService;

    @PostMapping("/search")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> searchProducts(
            @RequestBody(required = false) ProductSearchRequest request) {
        log.info("Received product search request: {}", request);

        List<ProductResponse> response = productService.searchProducts(request);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Products fetched successfully",
                        response));
    }
}