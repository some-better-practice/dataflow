package com.alan.dataflow.pipeline;

import com.alan.dataflow.dependency.DependencyTracker;
import com.alan.dataflow.domain.JobStatus;
import com.alan.dataflow.domain.ProcessingJob;
import com.alan.dataflow.event.StepCompletedEvent;
import com.alan.dataflow.logic.LogicRouter;
import com.alan.dataflow.logic.ProcessingLogicStrategy;
import com.alan.dataflow.pipeline.parser.ContentParserRegistry;
import com.alan.dataflow.pipeline.parser.ParsedContent;
import com.alan.dataflow.pipeline.parser.RawContent;
import com.alan.dataflow.storage.StorageService;
import com.alan.dataflow.storage.StorageZone;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CsvProcessingService {

    private final ContentParserRegistry parserRegistry;
    private final LogicRouter logicRouter;
    private final DependencyTracker dependencyTracker;
    private final StorageService storage;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${dataflow.simulation.processing-delay-ms:1200}")
    private long delayMs;

    /**
     * 執行在 heavy-pool：資料處理可能數小時。
     * 流程：
     *   1. ContentParserRegistry 依 ChannelInfo 選 Parser，將 CSV 解析成正規化 ParsedContent
     *   2. LogicRouter 依 FileKey 選業務邏輯策略
     *   3. 執行策略，傳入 ParsedContent（已知欄位結構的資料列）
     */
    @Async("heavyPool")
    public void process(ProcessingJob job) {
        job.transition(JobStatus.PROCESSING);
        log.info("  ▶ [heavy-{}] PROCESS   {}", Thread.currentThread().getName(),
                job.getFileKey().toLogLabel());

        // Step 1：parse — channel 決定欄位定義與解析邏輯
        RawContent raw = new RawContent(job.getChannelInfo(), job.getStorageRef());
        ParsedContent parsed = parserRegistry.get(job.getChannelInfo()).parse(raw);
        log.info("    [PARSE] channel={} rows={}", parsed.channel(), parsed.rows().size());

        // Step 2：route — FileKey 決定業務邏輯
        ProcessingLogicStrategy strategy = logicRouter.route(job.getFileKey());
        log.info("    [ROUTE] strategy={}", strategy.name());

        sleep(delayMs);

        // Step 3：process — 傳入已解析的資料
        String outputRef = strategy.process(job, parsed);

        storage.move(outputRef, StorageZone.PROCESSED, job.getFileKey(),
                job.getOriginalFileName().replaceAll("\\.[^.]+$", "") + "_result.csv");

        job.transition(JobStatus.PROCESSED);
        log.info("  ✓ [heavy-{}] PROCESS   {} done", Thread.currentThread().getName(),
                job.getFileKey().toLogLabel());

        dependencyTracker.markCompleted(job.getJobId());
        eventPublisher.publishEvent(new StepCompletedEvent(this, job, JobStatus.PROCESSED));
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
