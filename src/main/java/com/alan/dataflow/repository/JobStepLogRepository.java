package com.alan.dataflow.repository;

import com.alan.dataflow.entity.JobStepLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JobStepLogRepository extends JpaRepository<JobStepLog, Long> {

    List<JobStepLog> findByJobIdOrderByRecordedAt(String jobId);
}
