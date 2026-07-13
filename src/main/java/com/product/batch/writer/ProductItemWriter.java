package com.product.batch.writer;

import com.product.batch.entity.Product;
import com.product.batch.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductItemWriter implements ItemWriter<Product> {

    private final ProductRepository productRepository;

    @Override
    public void write(@NonNull Chunk<? extends Product> chunk) {
        productRepository.saveAll(chunk.getItems());

        log.info("Saved {} valid product records into PRODUCTS table", chunk.size());
    }
}