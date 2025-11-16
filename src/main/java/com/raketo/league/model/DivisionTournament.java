package com.raketo.league.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "divisions_tournaments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DivisionTournament {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "division_id")
    private Division division;

    @ManyToOne(optional = false)
    @JoinColumn(name = "tournament_id")
    private Tournament tournament;
}
