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

    /**
     * Nullable: a finished game gets dissociated from its lobby at endGame time
     * so it survives in the profile/history even if the lobby is later deleted.
     */
    @OneToOne
    @JoinColumn(name = "lobbyId")
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

    /** "MAFIA_WIN", "VILLAGER_WIN", "JESTER_WIN" — null while in progress. */
    @Column
    private String winningTeam;

    /** True if this game came out of the ranked queue and should affect ELO. */
    @Column(nullable = false, columnDefinition = "boolean default false")
    private Boolean ranked = false;

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

    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Message> messages = new ArrayList<>();

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

    public String getWinningTeam() {
        return winningTeam;
    }

    public void setWinningTeam(String winningTeam) {
        this.winningTeam = winningTeam;
    }

    public Boolean getRanked() {
        return ranked != null && ranked;
    }

    public void setRanked(Boolean ranked) {
        this.ranked = ranked;
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

    public List<Message> getMessages() {
        return messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }
}
