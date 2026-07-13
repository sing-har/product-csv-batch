package com.product.batch.controller;

import com.product.batch.dto.response.ApiResponse;
import com.product.batch.dto.response.BatchJobResponse;
import com.product.batch.dto.response.ErrorRecordResponse;
import com.product.batch.dto.response.JobStatusResponse;
import com.product.batch.service.BatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/batch")
@RequiredArgsConstructor
@Slf4j
public class BatchController {

    private final BatchService batchService;

    @PostMapping(value = "/process-products", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<BatchJobResponse>> processProducts(
            @RequestParam("file") MultipartFile file) {
        log.info(
                "Received product CSV upload request. fileName={}, size={} bytes",
                file.getOriginalFilename(),
                file.getSize());

        BatchJobResponse response = batchService.startProductCsvJob(file);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Product CSV batch job started successfully",
                        response));
    }

    @GetMapping("/status/{jobId}")
    public ResponseEntity<ApiResponse<JobStatusResponse>> getJobStatus(
            @PathVariable Long jobId) {
        log.info("Received job status request for jobId={}", jobId);

        JobStatusResponse response = batchService.getJobStatus(jobId);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Batch job status fetched successfully",
                        response));
    }

    @GetMapping("/errors/{jobId}")
    public ResponseEntity<ApiResponse<List<ErrorRecordResponse>>> getErrorsByJobId(
            @PathVariable Long jobId) {
        log.info("Received error records request for jobId={}", jobId);

        List<ErrorRecordResponse> response = batchService.getErrorsByJobId(jobId);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Batch error records fetched successfully",
                        response));
    }
}