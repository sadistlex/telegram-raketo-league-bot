package com.raketo.league.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "matches")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "tour_id", nullable = false)
    private Tour tour;

    @ManyToOne
    @JoinColumn(name = "player1_id", nullable = false)
    private Player player1;

    @ManyToOne
    @JoinColumn(name = "player2_id", nullable = false)
    private Player player2;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private MatchStatus status = MatchStatus.SCHEDULED;

    private LocalDateTime scheduledDateTime;

    private LocalDateTime playedDateTime;

    @ManyToOne
    @JoinColumn(name = "winner_id")
    private Player winner;

    private String score;

    @Column(nullable = false)
    @Builder.Default
    private Integer postponementsUsed = 0;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isPlayedInAdvance = false;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt;

    public enum MatchStatus {
        SCHEDULED,
        CONFIRMING_TIME,
        CONFIRMED,
        IN_PROGRESS,
        COMPLETED,
        POSTPONED,
        CANCELLED,
        WALKOVER
    }
}

