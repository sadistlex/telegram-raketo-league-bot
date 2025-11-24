package com.raketo.league.model;

import com.raketo.league.audit.AuditEntityListener;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "tours")
@EntityListeners(AuditEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tour {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "tour_template_id")
    private TourTemplate tourTemplate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TourStatus status = TourStatus.Active;

    @Column(name = "scheduled_time")
    private LocalDateTime scheduledTime;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "complete_date")
    private LocalDateTime completeDate;

    @ManyToOne
    @JoinColumn(name = "responsible_player_id")
    private Player responsiblePlayer;

    public enum TourStatus { Active, Scheduled, Walkover, Completed, Cancelled }
}
