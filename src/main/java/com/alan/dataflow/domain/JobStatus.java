package com.alan.dataflow.domain;

public enum JobStatus {
    PENDING,
    DOWNLOADING,
    DOWNLOADED,
    COMPRESSING,
    COMPRESSED,
    WAITING_DEPENDENCY,  // 等相依檔案完成
    CONVERTING,          // 執行外部編譯執行檔做格式轉換
    CONVERTED,
    PROCESSING,          // CSV 資料處理（可能數小時）
    PROCESSED,
    UPLOADING,
    DONE,
    FAILED
}
