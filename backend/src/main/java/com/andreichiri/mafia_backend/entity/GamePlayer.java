package com.andreichiri.mafia_backend.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "game_players")
public class GamePlayer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private MafiaUser user;

    private Role role;

    @Column(nullable = false)
    private Boolean alive = true;

    @Column
    private Integer killedAtRound;

    @Column
    @Enumerated(EnumType.STRING)
    private DeathCause deathCause;

    private enum Role {
        MAFIA,
        VILLAGER,
        SHERIFF
    }

    public enum DeathCause {
        MAFIA_KILL,
        VOTED_OUT,
        DISCONNECTED
    }
}
