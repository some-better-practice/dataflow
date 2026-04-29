package com.alan.dataflow.tracking;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * 每個 ProcessingJob 的生命週期摘要，每步驟 upsert 一次。
 */
@Entity
@Table(name = "job_record", indexes = {
        @Index(name = "idx_job_record_customer_factory", columnList = "customer_id,factory_id"),
        @Index(name = "idx_job_record_status", columnList = "current_status")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 應用層唯一 ID，對應 ProcessingJob.jobId */
    @Column(name = "job_id", nullable = false, unique = true, length = 64)
    private String jobId;

    // ── FileKey 五維鍵 ────────────────────────────────────────────────────
    @Column(name = "customer_id", nullable = false, length = 32)
    private String customerId;

    @Column(name = "product_id", nullable = false, length = 32)
    private String productId;

    @Column(name = "factory_id", nullable = false, length = 32)
    private String factoryId;

    @Column(name = "data_type", nullable = false, length = 32)
    private String dataType;

    @Column(name = "sub_type", nullable = false, length = 32)
    private String subType;

    // ── 其他 Job 屬性 ─────────────────────────────────────────────────────
    @Column(name = "channel", nullable = false, length = 16)
    private String channel;

    @Column(name = "original_file", length = 255)
    private String originalFile;

    /** 最新一次的 claim-check storage reference */
    @Column(name = "current_ref", length = 500)
    private String currentRef;

    /** 最新狀態，對應 JobStatus enum name */
    @Column(name = "current_status", nullable = false, length = 32)
    private String currentStatus;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
