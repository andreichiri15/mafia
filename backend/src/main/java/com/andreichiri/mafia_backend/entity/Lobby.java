package com.andreichiri.mafia_backend.entity;

import jakarta.annotation.Nullable;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "lobbies")
public class Lobby {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @ManyToOne
    @JoinColumn(name = "host_id", nullable = false)
    private MafiaUser host;

    @Column(nullable = false)
    private int maxPlayers;

    @Column
    private String password;

    @Column(nullable = false)
    private boolean isLocked;

    @Column
    private String generatedLink;

    @Column(nullable = false)
    private boolean publicLobby;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    // TODO: ceva legat de configurarea jocului (roluri, cat dureaza ziua, noaptea etc)

    // TODO: relatii

    @OneToMany(mappedBy = "id", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LobbyPlayer> lobbyPlayers = new ArrayList<>();

    @OneToOne(mappedBy = "lobby")
    private Game game;

    @OneToMany(mappedBy = "lobby")
    private List<Message> messages = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public MafiaUser getHost() {
        return host;
    }

    public void setHost(MafiaUser host) {
        this.host = host;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public void setMaxPlayers(int maxPlayers) {
        this.maxPlayers = maxPlayers;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isLocked() {
        return isLocked;
    }

    public void setLocked(boolean locked) {
        isLocked = locked;
    }

    public String getGeneratedLink() {
        return generatedLink;
    }

    public void setGeneratedLink(String generatedLink) {
        this.generatedLink = generatedLink;
    }

    public boolean isPublicLobby() {
        return publicLobby;
    }

    public void setPublicLobby(boolean publicLobby) {
        this.publicLobby = publicLobby;
    }

    public List<LobbyPlayer> getLobbyPlayers() {
        return lobbyPlayers;
    }

    public void setLobbyPlayers(List<LobbyPlayer> lobbyPlayers) {
        this.lobbyPlayers = lobbyPlayers;
    }

    public Game getGame() {
        return game;
    }

    public void setGame(Game game) {
        this.game = game;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
