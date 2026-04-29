package com.alan.dataflow.dependency;

import com.alan.dataflow.domain.FileKey;

/**
 * 描述一條相依規則：jobId 必須等待 requiredJobId 完成後才能繼續
 */
public record DependencyRule(
        String waitingJobId,
        FileKey waitingKey,
        String requiredJobId,
        String description
) {}
