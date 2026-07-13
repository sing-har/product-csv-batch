package com.product.batch.service;

import com.product.batch.dto.response.BatchJobResponse;
import com.product.batch.dto.response.ErrorRecordResponse;
import com.product.batch.dto.response.JobStatusResponse;
import com.product.batch.entity.ErrorRecord;
import com.product.batch.repository.ErrorRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BatchService {

    private final JobLauncher jobLauncher;
    private final Job productCsvJob;
    private final JobExplorer jobExplorer;
    private final ErrorRecordRepository errorRecordRepository;

    @Value("${app.batch.upload-dir}")
    private String uploadDir;

    public BatchJobResponse startProductCsvJob(MultipartFile file) {
        validateCsvFile(file);

        Path savedFilePath = saveUploadedFile(file);

        try {
            log.info("Launching productCsvJob for file: {}", savedFilePath);

            JobExecution jobExecution = jobLauncher.run(
                    productCsvJob,
                    new JobParametersBuilder()
                            .addString("filePath", savedFilePath.toString())
                            .addString("originalFileName", file.getOriginalFilename())
                            .addLong("startTime", System.currentTimeMillis())
                            .toJobParameters());

            log.info(
                    "productCsvJob completed. jobExecutionId={}, status={}",
                    jobExecution.getId(),
                    jobExecution.getStatus());

            return new BatchJobResponse(
                    jobExecution.getId(),
                    jobExecution.getStatus().toString(),
                    file.getOriginalFilename());

        } catch (Exception exception) {
            log.error("Failed to launch productCsvJob for file: {}", savedFilePath, exception);

            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to start product CSV batch job");
        }
    }

    public JobStatusResponse getJobStatus(Long jobId) {
        JobExecution jobExecution = jobExplorer.getJobExecution(jobId);

        if (jobExecution == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "No batch job found for jobId: " + jobId);
        }

        return new JobStatusResponse(
                jobExecution.getId(),
                jobExecution.getJobInstance().getJobName(),
                jobExecution.getStatus().toString(),
                jobExecution.getStartTime(),
                jobExecution.getEndTime(),
                jobExecution.getExitStatus().getExitCode(),
                jobExecution.getExitStatus().getExitDescription());
    }

    public List<ErrorRecordResponse> getErrorsByJobId(Long jobId) {
        return errorRecordRepository.findByJobId(jobId)
                .stream()
                .map(this::toErrorRecordResponse)
                .toList();
    }

    private ErrorRecordResponse toErrorRecordResponse(ErrorRecord errorRecord) {
        return new ErrorRecordResponse(
                errorRecord.getId(),
                errorRecord.getJobId(),
                errorRecord.getRawData(),
                errorRecord.getErrorMessage(),
                errorRecord.getCreatedAt());
    }

    private void validateCsvFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "CSV file is required");
        }

        String originalFilename = file.getOriginalFilename();

        if (originalFilename == null || originalFilename.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "CSV file name is required");
        }

        String lowerCaseFileName = originalFilename.toLowerCase(Locale.ROOT);

        if (!lowerCaseFileName.endsWith(".csv")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only .csv files are allowed");
        }

        log.info(
                "CSV file validation passed. fileName={}, size={} bytes",
                originalFilename,
                file.getSize());
    }

    private Path saveUploadedFile(MultipartFile file) {
        try {
            Path uploadPath = Path.of(uploadDir.trim())
                    .toAbsolutePath()
                    .normalize();

            Files.createDirectories(uploadPath);

            String originalFilename = file.getOriginalFilename();

            String safeOriginalFilename = originalFilename == null
                    ? "products.csv"
                    : Path.of(originalFilename).getFileName().toString();

            String uniqueFileName = UUID.randomUUID() + "-" + safeOriginalFilename;

            Path targetPath = uploadPath.resolve(uniqueFileName).normalize();

            if (!targetPath.startsWith(uploadPath)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Invalid file path");
            }

            Files.copy(
                    file.getInputStream(),
                    targetPath,
                    StandardCopyOption.REPLACE_EXISTING);

            log.info("CSV file saved successfully at: {}", targetPath);

            return targetPath;

        } catch (IOException exception) {
            log.error("Failed to save uploaded CSV file", exception);

            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to save uploaded CSV file");
        }
    }
}