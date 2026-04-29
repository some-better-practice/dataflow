package com.alan.dataflow.worker;

/**
 * 每個 instance 的角色。
 * 透過 spring.profiles.active 指定，決定啟動哪些 worker 和 thread pool 大小。
 *
 * ALL 用於本機開發 / 小規模部署，一個 instance 跑全部 step。
 * 其他 role 各自部署成獨立 pod，數量依資源需求橫向擴展。
 */
public enum WorkerRole {
    ALL,          // dev / single-node
    DOWNLOADER,   // FTP download + compress  — I/O bound,  light thread pool
    CONVERTER,    // format conversion binary  — CPU bound,  heavy thread pool
    PROCESSOR,    // CSV processing            — memory bound, heavy thread pool
    UPLOADER      // FTP upload               — I/O bound,  light thread pool
}
