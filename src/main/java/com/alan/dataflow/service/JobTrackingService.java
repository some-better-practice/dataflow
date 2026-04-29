package com.alan.dataflow.service;

import com.alan.dataflow.domain.FileKey;
import com.alan.dataflow.domain.JobStatus;
import com.alan.dataflow.domain.ProcessingJob;
import com.alan.dataflow.entity.JobRecord;
import com.alan.dataflow.entity.JobStepLog;
import com.alan.dataflow.repository.JobRecordRepository;
import com.alan.dataflow.repository.JobStepLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class JobTrackingService {

    private final JobRecordRepository jobRecordRepository;
    private final JobStepLogRepository jobStepLogRepository;

    /**
     * 在 pipeline service 呼叫 eventPublisher.publishEvent() 之前呼叫。
     * Upsert job_record，並 append 一筆 job_step_log。
     */
    @Transactional
    public void recordStep(ProcessingJob job, JobStatus completedStep) {
        JobRecord record = jobRecordRepository.findByJobId(job.getJobId())
                .orElseGet(() -> newRecord(job));
        record.setCurrentRef(job.getStorageRef());
        record.setCurrentStatus(completedStep.name());
        record.setUpdatedAt(Instant.now());
        jobRecordRepository.save(record);

        jobStepLogRepository.save(JobStepLog.builder()
                .jobId(job.getJobId())
                .completedStep(completedStep.name())
                .storageRef(job.getStorageRef())
                .threadName(Thread.currentThread().getName())
                .recordedAt(Instant.now())
                .build());
    }

    /**
     * 在 DependencyTracker.markCompleted() 呼叫 publishEvent() 之前呼叫。
     * 僅 append step log（此時只有 waitingJobId，沒有完整 ProcessingJob）。
     */
    @Transactional
    public void recordDependencyReady(String waitingJobId) {
        jobStepLogRepository.save(JobStepLog.builder()
                .jobId(waitingJobId)
                .completedStep("DEPENDENCY_RESOLVED")
                .threadName(Thread.currentThread().getName())
                .recordedAt(Instant.now())
                .build());
    }

    private JobRecord newRecord(ProcessingJob job) {
        FileKey k = job.getFileKey();
        return JobRecord.builder()
                .jobId(job.getJobId())
                .customerId(k.customerId())
                .productId(k.productId())
                .factoryId(k.factoryId())
                .dataType(k.dataType())
                .subType(k.subType())
                .channel(job.getChannelInfo().name())
                .originalFile(job.getOriginalFileName())
                .createdAt(job.getCreatedAt())
                .updatedAt(Instant.now())
                .build();
    }
}
