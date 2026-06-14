package com.product.batch.writer;

import com.product.batch.entity.Product;
import com.product.batch.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;
import org.springframework.lang.NonNull;

@Component
@RequiredArgsConstructor
public class ProductItemWriter implements ItemWriter<Product> {

    private final ProductRepository productRepository;

    @Override
    public void write(@NonNull Chunk<? extends Product> chunk) {
        productRepository.saveAll(chunk.getItems());
    }
}