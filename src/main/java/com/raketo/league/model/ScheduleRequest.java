package com.raketo.league.model;

import com.raketo.league.audit.AuditEntityListener;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "schedule_requests")
@EntityListeners(AuditEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduleRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "tour_id")
    private Tour tour;

    @Column(name = "proposed_date", nullable = false)
    private java.time.LocalDate proposedDate;

    @Column(name = "proposed_hours", columnDefinition = "TEXT")
    private String proposedHours;

    @ManyToOne(optional = false)
    @JoinColumn(name = "initiator_player_id")
    private Player initiatorPlayer;

    @ManyToOne(optional = false)
    @JoinColumn(name = "recepient_player_id")
    private Player recipientPlayer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ScheduleStatus status = ScheduleStatus.Pending;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum ScheduleStatus { Pending, Accepted, Declined, Expired, Cancelled }
}
