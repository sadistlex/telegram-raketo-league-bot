package com.raketo.league.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tours_players")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TourPlayer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "tour_id")
    private Tour tour;

    @ManyToOne(optional = false)
    @JoinColumn(name = "player_id")
    private Player player;
}
