package com.raketo.league.audit;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long actionLogId;

    @Column(name = "table_name", nullable = false)
    private String tableName;

    @Column(nullable = false, length = 20)
    private String operation;

    @Column(columnDefinition = "TEXT")
    private String changes;

    @Column(name = "player_id")
    private Long playerId;

    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;
}

