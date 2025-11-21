package com.raketo.league.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "players")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Player {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "telegram_id", unique = true)
    private Long telegramId;

    @Column(name = "telegram_username", nullable = false, unique = true)
    private String telegramUsername;

    @Enumerated(EnumType.STRING)
    @Column(name = "language", nullable = false)
    @Builder.Default
    private Language language = Language.RU;
}
