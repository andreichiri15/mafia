package com.andreichiri.mafia_backend.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "mafia_users")
public class MafiaUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(name = "dateJoined", nullable = false, updatable = false)
    private LocalDateTime dateJoined;

    /** True for AI-controlled fill-in players. Bots cannot log in. */
    @Column(nullable = false, columnDefinition = "boolean default false")
    private Boolean isBot = false;

    /** Ranked ELO (single global rating). Starts at 1000. */
    @Column(nullable = false, columnDefinition = "integer default 1000")
    private Integer elo = 1000;

    /** True if the user can edit the ranked config. Flipped manually in DB for now. */
    @Column(nullable = false, columnDefinition = "boolean default false")
    private Boolean isManager = false;

    /** If set and in the future, the user can't join the ranked queue until this passes. */
    @Column
    private LocalDateTime rankedCooldownUntil;

    @PrePersist
    protected void onCreate() {
        this.dateJoined = LocalDateTime.now();
    }

    @OneToMany(mappedBy = "user")
    private List<GamePlayer> gamePlayers;

    @OneToMany(mappedBy = "sender")
    private List<Message> messages;

    @OneToMany(mappedBy = "user")
    private List<LobbyPlayer> lobbyPlayers = new ArrayList<>();

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public List<GamePlayer> getGamePlayers() {
        return gamePlayers;
    }

    public void setGamePlayers(List<GamePlayer> gamePlayers) {
        this.gamePlayers = gamePlayers;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }

    public List<LobbyPlayer> getLobbyPlayers() {
        return lobbyPlayers;
    }

    public void setLobbyPlayers(List<LobbyPlayer> lobbyPlayers) {
        this.lobbyPlayers = lobbyPlayers;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public LocalDateTime getDateJoined() {
        return dateJoined;
    }

    public void setDateJoined(LocalDateTime dateJoined) {
        this.dateJoined = dateJoined;
    }

    public Boolean getIsBot() {
        return isBot != null && isBot;
    }

    public void setIsBot(Boolean isBot) {
        this.isBot = isBot;
    }

    public Integer getElo() {
        return elo != null ? elo : 1000;
    }

    public void setElo(Integer elo) {
        this.elo = elo;
    }

    public Boolean getIsManager() {
        return isManager != null && isManager;
    }

    public void setIsManager(Boolean isManager) {
        this.isManager = isManager;
    }

    public LocalDateTime getRankedCooldownUntil() {
        return rankedCooldownUntil;
    }

    public void setRankedCooldownUntil(LocalDateTime rankedCooldownUntil) {
        this.rankedCooldownUntil = rankedCooldownUntil;
    }
}
