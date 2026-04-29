package com.alan.dataflow.worker;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

import java.util.Arrays;
import java.util.Map;

public class WorkerRoleCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        Map<String, Object> attrs = metadata.getAnnotationAttributes(OnWorkerRole.class.getName());
        if (attrs == null) return true;

        WorkerRole[] required = (WorkerRole[]) attrs.get("value");
        String active = context.getEnvironment().getProperty("dataflow.worker.role", "ALL");
        WorkerRole activeRole = WorkerRole.valueOf(active.toUpperCase());

        return Arrays.asList(required).contains(activeRole);
    }
}
