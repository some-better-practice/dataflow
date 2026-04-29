package com.alan.dataflow.repository;

import com.alan.dataflow.entity.JobRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JobRecordRepository extends JpaRepository<JobRecord, Long> {

    Optional<JobRecord> findByJobId(String jobId);
}
