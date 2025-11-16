package com.raketo.league.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "divisions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Division {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column
    private Integer level;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "chat_id")
    private Long chatId;
}
