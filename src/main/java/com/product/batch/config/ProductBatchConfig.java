package com.product.batch.config;

import com.product.batch.entity.Product;
import com.product.batch.model.ProductCsvRow;
import com.product.batch.processor.ProductItemProcessor;
import com.product.batch.writer.ProductItemWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Objects;

@Slf4j
@Configuration
public class ProductBatchConfig {

        @Bean
        @StepScope
        public FlatFileItemReader<ProductCsvRow> productCsvReader(
                        @Value("#{jobParameters['filePath']}") String filePath) {
                Objects.requireNonNull(filePath, "Input CSV file path is required");

                log.info("Configuring productCsvReader with uploaded file path: {}", filePath);

                return new FlatFileItemReaderBuilder<ProductCsvRow>()
                                .name("productCsvReader")
                                .resource(new FileSystemResource(filePath))
                                .linesToSkip(1)
                                .strict(true)
                                .delimited()
                                .delimiter(",")
                                .names("productId", "name", "category", "price", "quantity", "createdDate")
                                .targetType(ProductCsvRow.class)
                                .build();
        }

        @Bean
        public Step processProductCsvStep(
                        JobRepository jobRepository,
                        PlatformTransactionManager transactionManager,
                        FlatFileItemReader<ProductCsvRow> productCsvReader,
                        ProductItemProcessor productItemProcessor,
                        ProductItemWriter productItemWriter) {
                return new StepBuilder("processProductCsvStep", jobRepository)
                                .<ProductCsvRow, Product>chunk(50, transactionManager)
                                .reader(productCsvReader)
                                .processor(productItemProcessor)
                                .writer(productItemWriter)
                                .build();
        }

        @Bean
        public Job productCsvJob(
                        JobRepository jobRepository,
                        Step processProductCsvStep) {
                return new JobBuilder("productCsvJob", jobRepository)
                                .incrementer(new RunIdIncrementer())
                                .start(processProductCsvStep)
                                .build();
        }
}