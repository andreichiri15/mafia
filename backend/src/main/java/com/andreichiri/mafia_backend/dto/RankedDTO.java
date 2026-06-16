package com.andreichiri.mafia_backend.dto;

public class RankedDTO {

    public record RankedConfigDTO(
            Integer playerCount,
            Integer mafiaCount,
            boolean includeSheriff,
            boolean includeDoctor,
            boolean includeJester,
            boolean includeMutilator,
            Integer doctorSelfSaveLimit,
            Integer sheriffInvestigationDelay,
            Integer nightDurationSeconds,
            Integer dayDurationSeconds,
            Integer votingDurationSeconds
    ) {}

    public record QueueStatus(
            boolean inQueue,
            int queueSize,
            long enqueuedAtMs,
            Integer elo,
            Long cooldownUntilMs
    ) {}

    public record MatchFoundEvent(
            Long gameId
    ) {}

    public record LeaderboardEntry(
            Long userId,
            String username,
            Integer elo
    ) {}
}
