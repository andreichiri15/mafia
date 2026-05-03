package com.andreichiri.mafia_backend.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "game_players")
public class GamePlayer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gameId", nullable = false)
    private Game game;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId", nullable = false)
    private MafiaUser user;

    @Enumerated(EnumType.STRING)
    private Role role;

    @Column(nullable = false)
    private Boolean alive = true;

    @Column
    private Integer killedAtRound;

    @Column
    @Enumerated(EnumType.STRING)
    private DeathCause deathCause;

    @Column(nullable = false)
    private Integer selfHealsUsed = 0;

    @Column
    private Integer silencedUntilRound;

    @Column
    private Integer voteRevokedUntilRound;

    public enum Role {
        MAFIA,
        VILLAGER,
        SHERIFF,
        DOCTOR,
        JESTER,
        MUTILATOR;

        public boolean isMafiaSide() {
            return this == MAFIA || this == MUTILATOR;
        }
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

    public Integer getSelfHealsUsed() { return selfHealsUsed; }
    public void setSelfHealsUsed(Integer selfHealsUsed) { this.selfHealsUsed = selfHealsUsed; }

    public Integer getSilencedUntilRound() { return silencedUntilRound; }
    public void setSilencedUntilRound(Integer silencedUntilRound) { this.silencedUntilRound = silencedUntilRound; }

    public Integer getVoteRevokedUntilRound() { return voteRevokedUntilRound; }
    public void setVoteRevokedUntilRound(Integer voteRevokedUntilRound) { this.voteRevokedUntilRound = voteRevokedUntilRound; }
}
