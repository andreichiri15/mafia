package com.andreichiri.mafia_backend.dto;

import java.time.LocalDateTime;

public class LobbySummaryReport {
    public Long id;
    public String name;
    public String hostname;
    public Integer maxPlayers;
    public Integer currentPlayers;
    public LocalDateTime createdAt;

    public LobbySummaryReport(Long id, String name, String hostname, Integer maxPlayers, Integer currentPlayers, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.hostname = hostname;
        this.maxPlayers = maxPlayers;
        this.currentPlayers = currentPlayers;
        this.createdAt = createdAt;
    }

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

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public Integer getMaxPlayers() {
        return maxPlayers;
    }

    public void setMaxPlayers(Integer maxPlayers) {
        this.maxPlayers = maxPlayers;
    }

    public Integer getCurrentPlayers() {
        return currentPlayers;
    }

    public void setCurrentPlayers(Integer currentPlayers) {
        this.currentPlayers = currentPlayers;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
