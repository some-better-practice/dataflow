package com.alan.dataflow.tracking;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JobStepLogRepository extends JpaRepository<JobStepLog, Long> {

    List<JobStepLog> findByJobIdOrderByRecordedAt(String jobId);
}
