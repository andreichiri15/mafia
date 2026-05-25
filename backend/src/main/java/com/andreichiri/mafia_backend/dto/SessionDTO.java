package com.andreichiri.mafia_backend.dto;

public class SessionDTO {

    public record SessionInfo(
            Long lobbyId,
            String lobbyName,
            Long gameId,        // null if no game has been started yet
            String gamePhase,   // null if no game; otherwise NIGHT/DAY/VOTING (GAME_OVER never returned)
            boolean alive       // only meaningful when gameId != null
    ) {}
}
