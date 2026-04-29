package com.alan.dataflow.storage;

public enum StorageZone {
    /** MinIO: 原始未動過的壓縮檔，source of truth */
    RAW,
    /** MinIO: 格式轉換後的 csv */
    STAGING,
    /** NFS: 處理中的中間產物（速度優先） */
    WORKING,
    /** MinIO: 最終結果，等待上傳到下游 FTP */
    PROCESSED,
    /** MinIO Glacier-class: 長期備份 */
    ARCHIVE
}
