package com.andreichiri.mafia_backend.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class ProfileDTO {

    public record RoleStats(
            int played,
            int won,
            double winRate
    ) {}

    public record MatchHistoryEntry(
            Long gameId,
            String lobbyName,
            String role,
            String winningTeam,
            boolean won,
            boolean alive,
            String deathCause,
            LocalDateTime startedAt,
            LocalDateTime endedAt,
            long durationSeconds
    ) {}

    public record ProfileResponse(
            Long userId,
            String username,
            LocalDateTime dateJoined,
            int totalGames,
            int totalWins,
            double winRate,
            String favoriteRole,
            double survivalRate,
            long avgGameDurationSeconds,
            Map<String, RoleStats> roleStats,
            List<MatchHistoryEntry> matchHistory
    ) {}
}
