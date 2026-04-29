package com.alan.dataflow.pipeline.parser;

import com.alan.dataflow.constants.ChannelInfo;

/**
 * Parser 的輸入：channel 識別 + 已轉成 CSV 的原始字串
 * （格式轉換在 FormatConversionService 已完成）
 */
public record RawContent(
        ChannelInfo channel,
        String csvData
) {}
