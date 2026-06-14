package com.product.batch.config;

import com.product.batch.model.ProductCsvRow;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.util.Objects;

@Configuration
public class ProductBatchConfig {

    @Bean
    @StepScope
    public FlatFileItemReader<ProductCsvRow> productCsvReader(
            @Value("${app.batch.input-file}") Resource inputFile) {
        return new FlatFileItemReaderBuilder<ProductCsvRow>()
                .name("productCsvReader")
                .resource(Objects.requireNonNull(inputFile, "Input CSV file is required"))
                .linesToSkip(1)
                .strict(true)
                .delimited()
                .delimiter(",")
                .names("productId", "name", "category", "price", "quantity", "createdDate")
                .targetType(ProductCsvRow.class)
                .build();
    }
}