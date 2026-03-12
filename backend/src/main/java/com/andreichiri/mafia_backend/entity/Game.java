package com.andreichiri.mafia_backend.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GamePlayer> gamePlayers = new ArrayList<>();

    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL)
    private List<GameAction> actions = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Lobby getLobby() {
        return lobby;
    }

    public void setLobby(Lobby lobby) {
        this.lobby = lobby;
    }

    public Integer getCurrentRound() {
        return currentRound;
    }

    public void setCurrentRound(Integer currentRound) {
        this.currentRound = currentRound;
    }

    public GamePhase getGamePhase() {
        return gamePhase;
    }

    public void setGamePhase(GamePhase gamePhase) {
        this.gamePhase = gamePhase;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(LocalDateTime endedAt) {
        this.endedAt = endedAt;
    }

    public List<GamePlayer> getGamePlayers() {
        return gamePlayers;
    }

    public void setGamePlayers(List<GamePlayer> gamePlayers) {
        this.gamePlayers = gamePlayers;
    }

    public List<GameAction> getActions() {
        return actions;
    }

    public void setActions(List<GameAction> actions) {
        this.actions = actions;
    }
}
