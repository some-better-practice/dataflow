package com.alan.dataflow.tracking;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Append-only 步驟稽核日誌。
 * 每次 eventPublisher.publishEvent() 之前寫入一筆，不可修改。
 */
@Entity
@Table(name = "job_step_log", indexes = {
        @Index(name = "idx_step_log_job_id", columnList = "job_id"),
        @Index(name = "idx_step_log_recorded_at", columnList = "recorded_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobStepLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 對應 job_record.job_id，不設 FK 以降低鎖競爭 */
    @Column(name = "job_id", nullable = false, length = 64)
    private String jobId;

    /** 剛完成的步驟，對應 JobStatus enum name 或 "DEPENDENCY_RESOLVED" */
    @Column(name = "completed_step", nullable = false, length = 32)
    private String completedStep;

    /** 步驟完成時的 storage reference（claim-check 路徑） */
    @Column(name = "storage_ref", length = 500)
    private String storageRef;

    /** 執行此步驟的執行緒名稱，用於對應 thread pool */
    @Column(name = "thread_name", length = 64)
    private String threadName;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;
}
