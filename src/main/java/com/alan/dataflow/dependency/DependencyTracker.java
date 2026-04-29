package com.alan.dataflow.dependency;

import com.alan.dataflow.event.DependencyReadyEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 管理 job 間的相依性。
 *
 * 場景：WAT 資料處理依賴同週期的 SPC 資料先完成（某些廠區規則）。
 * 當 SPC job 完成時，此 tracker 檢查有無被 unblock 的 WAT job。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DependencyTracker {

    private final ApplicationEventPublisher eventPublisher;

    // completedJobId -> 所有等待它的 jobId
    private final Map<String, List<String>> waitMap = new ConcurrentHashMap<>();
    // waitingJobId -> 還剩幾個相依未完成
    private final Map<String, Integer> pendingCount = new ConcurrentHashMap<>();

    public void register(DependencyRule rule) {
        log.info("  [DEPENDENCY] Register: job {} waits for job {} ({})",
                rule.waitingJobId(), rule.requiredJobId(), rule.description());
        waitMap.computeIfAbsent(rule.requiredJobId(), k -> new ArrayList<>())
               .add(rule.waitingJobId());
        pendingCount.merge(rule.waitingJobId(), 1, Integer::sum);
    }

    public boolean isBlocked(String jobId) {
        return pendingCount.getOrDefault(jobId, 0) > 0;
    }

    /** 某 job 完成後呼叫，解除相依並發 DependencyReadyEvent */
    public void markCompleted(String completedJobId) {
        List<String> unblocked = waitMap.getOrDefault(completedJobId, List.of());
        for (String waitingJobId : unblocked) {
            int remaining = pendingCount.merge(waitingJobId, -1, Integer::sum);
            if (remaining <= 0) {
                pendingCount.remove(waitingJobId);
                log.info("  [DEPENDENCY] ✓ job {} unblocked (required {} completed)",
                        waitingJobId, completedJobId);
                eventPublisher.publishEvent(new DependencyReadyEvent(this, waitingJobId));
            }
        }
    }
}
