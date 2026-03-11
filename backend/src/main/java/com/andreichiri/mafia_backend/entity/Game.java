package com.andreichiri.mafia_backend.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "games")
public class Game {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "lobby_id", nullable = false)
    private Lobby lobby;

    @Column(nullable = false)
    private Integer currentRound;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private GamePhase gamePhase;

    @Column(nullable = false, updatable = false)
    private LocalDateTime startedAt = LocalDateTime.now();

    @Column
    private LocalDateTime endedAt;

    public enum GamePhase {
        NIGHT,
        DAY,
        VOTING,
        GAME_OVER
    }
}
