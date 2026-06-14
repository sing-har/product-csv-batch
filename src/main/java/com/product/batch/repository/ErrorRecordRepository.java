package com.product.batch.repository;

import com.product.batch.entity.ErrorRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ErrorRecordRepository extends JpaRepository<ErrorRecord, Long> {

    List<ErrorRecord> findByJobId(Long jobId);

}