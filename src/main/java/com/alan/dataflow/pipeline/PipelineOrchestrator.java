package com.alan.dataflow.pipeline;

import com.alan.dataflow.dependency.DependencyTracker;
import com.alan.dataflow.domain.JobStatus;
import com.alan.dataflow.domain.ProcessingJob;
import com.alan.dataflow.event.DependencyReadyEvent;
import com.alan.dataflow.event.FileDetectedEvent;
import com.alan.dataflow.event.StepCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 模擬 NATS JetStream consumer + Temporal workflow orchestration。
 *
 * 職責：
 *   - 監聽 FileDetectedEvent → 啟動 download
 *   - 監聽 StepCompletedEvent → 根據當前狀態決定下一步
 *   - 監聽 DependencyReadyEvent → 從 WAITING_DEPENDENCY 繼續
 *
 * 每個 EventListener 呼叫都是在 caller 的 thread 上同步執行，
 * 但各 pipeline step 內部都 @Async，所以實際工作跑在對應的 worker pool。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PipelineOrchestrator {

    private final FtpDownloadService downloadService;
    private final CompressionService compressionService;
    private final FormatConversionService conversionService;
    private final CsvProcessingService processingService;
    private final FtpUploadService uploadService;
    private final DependencyTracker dependencyTracker;

    // jobId → job，讓 DependencyReadyEvent 能找回 job
    private final Map<String, ProcessingJob> jobRegistry = new ConcurrentHashMap<>();

    @EventListener
    public void onFileDetected(FileDetectedEvent event) {
        ProcessingJob job = event.getJob();
        jobRegistry.put(job.getJobId(), job);
        log.info("◆ [NATS] file.detected  {} jobId={} file={}",
                job.getFileKey().toLogLabel(), job.getJobId(), job.getOriginalFileName());
        downloadService.download(job);
    }

    @EventListener
    public void onStepCompleted(StepCompletedEvent event) {
        ProcessingJob job = event.getJob();
        JobStatus completed = event.getCompletedStep();

        log.info("◆ [NATS] step.completed {} jobId={} step={}",
                job.getFileKey().toLogLabel(), job.getJobId(), completed);

        switch (completed) {
            case DOWNLOADED -> compressionService.compressAndBackup(job);

            case COMPRESSED -> {
                if (dependencyTracker.isBlocked(job.getJobId())) {
                    job.transition(JobStatus.WAITING_DEPENDENCY);
                    log.warn("  ⏸  [DEPENDENCY] {} is parked — waiting for required job(s)",
                            job.getJobId());
                } else {
                    conversionService.convert(job);
                }
            }

            case CONVERTED -> processingService.process(job);

            case PROCESSED -> uploadService.upload(job);

            case DONE -> log.info("◆ [PIPELINE] {} COMPLETE ✓✓✓", job.getJobId());

            default -> log.debug("  (no transition for step {})", completed);
        }
    }

    @EventListener
    public void onDependencyReady(DependencyReadyEvent event) {
        String jobId = event.getUnblockedJobId();
        ProcessingJob job = jobRegistry.get(jobId);
        if (job == null) {
            log.error("  [DEPENDENCY] job {} not found in registry!", jobId);
            return;
        }
        log.info("◆ [NATS] dependency.ready jobId={} → resuming from WAITING_DEPENDENCY", jobId);
        conversionService.convert(job);
    }
}
