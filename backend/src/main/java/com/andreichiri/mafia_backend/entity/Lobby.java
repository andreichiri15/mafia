package com.andreichiri.mafia_backend.entity;

import jakarta.annotation.Nullable;
import jakarta.persistence.*;

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
    private boolean isPublic;

    // TODO: ceva legat de configurarea jocului (roluri, cat dureaza ziua, noaptea etc)

    // TODO: relatii

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

    public boolean isPublic() {
        return isPublic;
    }

    public void setPublic(boolean aPublic) {
        isPublic = aPublic;
    }
}
