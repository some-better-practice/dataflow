package com.alan.dataflow.config;

import com.alan.dataflow.worker.WorkerRole;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Thread pool 大小依 dataflow.worker.role 決定。
 *
 * DOWNLOADER/UPLOADER  → light pool 大、heavy pool 最小
 * CONVERTER            → heavy pool = CPU 核心數（CPU-bound）
 * PROCESSOR            → heavy pool = CPU * 2（memory-bound 可超訂）
 * ALL                  → dev 預設，各開適中大小
 */
@Configuration
public class WorkerPoolConfig {

    @Value("${dataflow.worker.role:ALL}")
    private String roleName;

    @Bean("lightPool")
    public Executor lightPool() {
        int size = switch (role()) {
            case DOWNLOADER, UPLOADER -> 16;
            case CONVERTER, PROCESSOR -> 2;
            case ALL                  -> 8;
        };
        return buildPool("light-", size, 500);
    }

    @Bean("heavyPool")
    public Executor heavyPool() {
        int size = switch (role()) {
            case CONVERTER  -> Runtime.getRuntime().availableProcessors();
            case PROCESSOR  -> Runtime.getRuntime().availableProcessors() * 2;
            case DOWNLOADER, UPLOADER -> 1;
            case ALL        -> 3;
        };
        return buildPool("heavy-", size, 50);
    }

    @Bean("aggregationPool")
    public Executor aggregationPool() {
        return buildPool("aggr-", 2, 20);
    }

    private WorkerRole role() {
        return WorkerRole.valueOf(roleName.toUpperCase());
    }

    private Executor buildPool(String prefix, int size, int queueCapacity) {
        var exec = new ThreadPoolTaskExecutor();
        exec.setCorePoolSize(size);
        exec.setMaxPoolSize(size);
        exec.setThreadNamePrefix(prefix);
        exec.setQueueCapacity(queueCapacity);
        exec.initialize();
        return exec;
    }
}
