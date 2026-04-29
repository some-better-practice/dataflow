package com.alan.dataflow.domain;

/**
 * 五維複合鍵：決定資料處理邏輯路由
 * customer / product / factory / dataType / subType
 */
public record FileKey(
        String customerId,   // e.g. ALAN
        String productId,    // e.g. N5, N3
        String factoryId,    // e.g. FAB12, FAB14, FAB18
        String dataType,     // e.g. WAT, SPC, PCM
        String subType       // e.g. INLINE, FINAL — 同檔案內不同資料段
) {
    public String toRoutingKey() {
        return String.join(".", customerId, productId, factoryId, dataType, subType);
    }

    public String toLogLabel() {
        return "[%s|%s|%s|%s|%s]".formatted(customerId, productId, factoryId, dataType, subType);
    }
}
