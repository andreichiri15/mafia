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

    public MafiaUser getUser() {
        return user;
    }

    public void setUser(MafiaUser user) {
        this.user = user;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public Boolean getAlive() {
        return alive;
    }

    public void setAlive(Boolean alive) {
        this.alive = alive;
    }

    public Integer getKilledAtRound() {
        return killedAtRound;
    }

    public void setKilledAtRound(Integer killedAtRound) {
        this.killedAtRound = killedAtRound;
    }

    public DeathCause getDeathCause() {
        return deathCause;
    }

    public void setDeathCause(DeathCause deathCause) {
        this.deathCause = deathCause;
    }
}
