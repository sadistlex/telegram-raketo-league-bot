package com.raketo.league.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.raketo.league.util.PlayerContextHolder;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class AuditService {
    private static final Logger logger = LoggerFactory.getLogger(AuditService.class);
    private final AuditLogRepository repository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void record(String tableName, String operation, Object entity) {
        Long playerId = PlayerContextHolder.getCurrentPlayerId();
        try {
            String snapshot = objectMapper.writeValueAsString(entity);
            AuditLog log = AuditLog.builder()
                    .tableName(tableName)
                    .operation(operation)
                    .changes(snapshot)
                    .playerId(playerId)
                    .updateTime(LocalDateTime.now())
                    .build();
            repository.save(log);
        } catch (Exception e) {
            logger.error("Failed to write audit log", e);
        }
    }
}
