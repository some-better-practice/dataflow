package com.alan.dataflow.logic;

import com.alan.dataflow.domain.FileKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 根據 FileKey 的 routing key 選出對應策略
 * 比對規則：完全符合 > 萬用字元 (*) 符合 > 拋 exception
 */
@Slf4j
@Service
public class LogicRouter {

    private final List<ProcessingLogicStrategy> strategies;

    public LogicRouter(List<ProcessingLogicStrategy> strategies) {
        this.strategies = strategies;
        strategies.forEach(s ->
            log.info("  [LOGIC-ROUTER] Registered strategy: {} → pattern={}",
                     s.name(), s.routingPattern()));
    }

    public ProcessingLogicStrategy route(FileKey key) {
        String routingKey = key.toRoutingKey();

        // 優先完全符合
        for (ProcessingLogicStrategy s : strategies) {
            if (s.routingPattern().equals(routingKey)) {
                return s;
            }
        }

        // 萬用字元比對（* 代表任何單一 segment）
        for (ProcessingLogicStrategy s : strategies) {
            if (matches(s.routingPattern(), routingKey)) {
                log.debug("  [LOGIC-ROUTER] {} → wildcard match: {}", routingKey, s.routingPattern());
                return s;
            }
        }

        throw new IllegalStateException(
            "No processing logic found for key: " + routingKey);
    }

    private boolean matches(String pattern, String key) {
        String[] patParts = pattern.split("\\.");
        String[] keyParts = key.split("\\.");
        if (patParts.length != keyParts.length) return false;
        for (int i = 0; i < patParts.length; i++) {
            if (!"*".equals(patParts[i]) && !patParts[i].equals(keyParts[i])) return false;
        }
        return true;
    }
}
