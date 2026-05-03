package com.andreichiri.mafia_backend.entity;

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
    @JoinColumn(name = "hostId", nullable = false)
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

    // Game configuration (host-editable, used at game start)
    @Column(nullable = false)
    private Integer mafiaCount = 1;

    @Column(nullable = false)
    private boolean includeSheriff = true;

    @Column(nullable = false)
    private boolean includeDoctor = false;

    @Column(nullable = false)
    private boolean includeJester = false;

    @Column(nullable = false)
    private boolean includeMutilator = false;

    /** -1 means unlimited self-saves. */
    @Column(nullable = false)
    private Integer doctorSelfSaveLimit = -1;

    /** Sheriff cannot investigate during the first N rounds. 0 = no delay. */
    @Column(nullable = false)
    private Integer sheriffInvestigationDelay = 0;

    @OneToMany(mappedBy = "lobby", cascade = CascadeType.ALL, orphanRemoval = true)
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

    public Integer getMafiaCount() { return mafiaCount; }
    public void setMafiaCount(Integer mafiaCount) { this.mafiaCount = mafiaCount; }

    public boolean isIncludeSheriff() { return includeSheriff; }
    public void setIncludeSheriff(boolean includeSheriff) { this.includeSheriff = includeSheriff; }

    public boolean isIncludeDoctor() { return includeDoctor; }
    public void setIncludeDoctor(boolean includeDoctor) { this.includeDoctor = includeDoctor; }

    public boolean isIncludeJester() { return includeJester; }
    public void setIncludeJester(boolean includeJester) { this.includeJester = includeJester; }

    public boolean isIncludeMutilator() { return includeMutilator; }
    public void setIncludeMutilator(boolean includeMutilator) { this.includeMutilator = includeMutilator; }

    public Integer getDoctorSelfSaveLimit() { return doctorSelfSaveLimit; }
    public void setDoctorSelfSaveLimit(Integer doctorSelfSaveLimit) { this.doctorSelfSaveLimit = doctorSelfSaveLimit; }

    public Integer getSheriffInvestigationDelay() { return sheriffInvestigationDelay; }
    public void setSheriffInvestigationDelay(Integer sheriffInvestigationDelay) { this.sheriffInvestigationDelay = sheriffInvestigationDelay; }
}
