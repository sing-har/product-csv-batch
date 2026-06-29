package com.product.batch.controller;

import com.product.batch.entity.ErrorRecord;
import com.product.batch.service.BatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.JobExecution;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/batch")
@RequiredArgsConstructor
public class BatchController {

    private final BatchService batchService;

    @PostMapping("/process-products")
    public ResponseEntity<Map<String, Object>> processProducts() throws Exception {

        JobExecution jobExecution = batchService.startProductCsvJob();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", "Product CSV batch job started successfully");
        response.put("jobExecutionId", jobExecution.getId());
        response.put("jobStatus", jobExecution.getStatus().toString());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/status/{jobId}")
    public ResponseEntity<Map<String, Object>> getJobStatus(@PathVariable Long jobId) {
        return ResponseEntity.ok(batchService.getJobStatus(jobId));
    }

    @GetMapping("/errors/{jobId}")
    public ResponseEntity<List<ErrorRecord>> getErrorsByJobId(@PathVariable Long jobId) {
        return ResponseEntity.ok(batchService.getErrorsByJobId(jobId));
    }
}