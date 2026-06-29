package com.product.batch.service;

import com.product.batch.entity.ErrorRecord;
import com.product.batch.repository.ErrorRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
public class BatchService {

    private final JobLauncher jobLauncher;
    private final Job productCsvJob;
    private final JobExplorer jobExplorer;
    private final ErrorRecordRepository errorRecordRepository;

    public JobExecution startProductCsvJob() throws Exception {
        return jobLauncher.run(
                productCsvJob,
                new JobParametersBuilder()
                        .addLong("startTime", System.currentTimeMillis())
                        .toJobParameters());
    }

    public Map<String, Object> getJobStatus(Long jobId) {
        JobExecution jobExecution = jobExplorer.getJobExecution(jobId);

        if (jobExecution == null) {
            throw new ResponseStatusException(NOT_FOUND, "No batch job found for jobId: " + jobId);
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("jobExecutionId", jobExecution.getId());
        response.put("jobName", jobExecution.getJobInstance().getJobName());
        response.put("status", jobExecution.getStatus().toString());
        response.put("startTime", jobExecution.getStartTime());
        response.put("endTime", jobExecution.getEndTime());
        response.put("exitCode", jobExecution.getExitStatus().getExitCode());
        response.put("exitMessage", jobExecution.getExitStatus().getExitDescription());

        return response;
    }

    public List<ErrorRecord> getErrorsByJobId(Long jobId) {
        return errorRecordRepository.findByJobId(jobId);
    }
}