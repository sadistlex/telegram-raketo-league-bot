package com.raketo.league.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "players_divisions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlayerDivisionAssignment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "player_id")
    private Player player;

    @ManyToOne(optional = false)
    @JoinColumn(name = "divisions_tournaments_id")
    private DivisionTournament divisionTournament;
}
