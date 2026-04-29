package com.alan.dataflow.worker;

import org.springframework.context.annotation.Conditional;

import java.lang.annotation.*;

/**
 * 標記某個 Bean 只在指定 WorkerRole 下啟動。
 *
 * 用法：
 *   @OnWorkerRole({WorkerRole.CONVERTER, WorkerRole.ALL})
 *   @Service
 *   public class FormatConversionService { ... }
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Conditional(WorkerRoleCondition.class)
public @interface OnWorkerRole {
    WorkerRole[] value();
}
