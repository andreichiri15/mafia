package com.andreichiri.mafia_backend.entity;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "game_actions")
public class GameAction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @Column(nullable = false)
    private Integer round;

    @Column(nullable = false)
    private Game.GamePhase gamePhase;

    @ManyToOne
    @JoinColumn(name = "actor_id", nullable = false)
    private MafiaUser actor;

    @ManyToOne
    @JoinColumn(name = "target_id", nullable = false)
    private MafiaUser target;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ActionType actionType;

    @Column
    private String result;

    @Column(nullable = false)
    private LocalDateTime executedAt;

    public enum ActionType {
        VOTE,
        MAFIA_KILL,
        HEALED,
        INVESTIGATE
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Game getGame() {
        return game;
    }

    public void setGame(Game game) {
        this.game = game;
    }

    public Integer getRound() {
        return round;
    }

    public void setRound(Integer round) {
        this.round = round;
    }

    public Game.GamePhase getGamePhase() {
        return gamePhase;
    }

    public void setGamePhase(Game.GamePhase gamePhase) {
        this.gamePhase = gamePhase;
    }

    public MafiaUser getActor() {
        return actor;
    }

    public void setActor(MafiaUser actor) {
        this.actor = actor;
    }

    public MafiaUser getTarget() {
        return target;
    }

    public void setTarget(MafiaUser target) {
        this.target = target;
    }

    public ActionType getActionType() {
        return actionType;
    }

    public void setActionType(ActionType actionType) {
        this.actionType = actionType;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public LocalDateTime getExecutedAt() {
        return executedAt;
    }

    public void setExecutedAt(LocalDateTime executedAt) {
        this.executedAt = executedAt;
    }
}
