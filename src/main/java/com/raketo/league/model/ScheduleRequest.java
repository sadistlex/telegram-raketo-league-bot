package com.raketo.league.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "schedule_requests")
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

    @Column(name = "proposed_time", nullable = false)
    private LocalDateTime proposedTime;

    @ManyToOne(optional = false)
    @JoinColumn(name = "initiator_player_id")
    private Player initiatorPlayer;

    @ManyToOne(optional = false)
    @JoinColumn(name = "recepient_player_id")
    private Player recipientPlayer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ScheduleStatus status = ScheduleStatus.Pending;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum ScheduleStatus { Pending, Accepted, Declined, Expired, Cancelled }
}
