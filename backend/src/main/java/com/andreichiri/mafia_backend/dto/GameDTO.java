package com.andreichiri.mafia_backend.dto;

import com.andreichiri.mafia_backend.entity.Game;
import com.andreichiri.mafia_backend.entity.GamePlayer;

import java.util.List;

public class GameDTO {

    public record GameStateResponse(
            Long gameId,
            Game.GamePhase phase,
            int round,
            long phaseEndTime,
            GamePlayer.Role yourRole,
            boolean alive,
            List<GamePlayerInfo> players,
            List<String> events
    ) {}

    public record GamePlayerInfo(
            Long userId,
            String username,
            boolean alive,
            GamePlayer.Role role // null unless game over or same team
    ) {}

    public record ActionRequest(
            Long targetUserId,
            String actionType // MAFIA_KILL, INVESTIGATE, VOTE
    ) {}

    public record PhaseResultEvent(
            Game.GamePhase phase,
            int round,
            String eliminatedPlayer, // null if no one eliminated
            List<String> events
    ) {}

    public record GameOverEvent(
            String winningTeam, // MAFIA_WIN or VILLAGER_WIN
            List<GamePlayerInfo> players // all roles revealed
    ) {}

    public record GameStartEvent(
            Long gameId,
            Long lobbyId
    ) {}
}
