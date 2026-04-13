package com.andreichiri.mafia_backend.dto;

import java.time.LocalDateTime;
import java.util.List;

public class LobbyDTO {
    public record CreateLobbyRequest(
            String name,
            Integer maxPlayers,
            String password,
            boolean isLocked,
            boolean publicLobby
    ) {
        public CreateLobbyRequest {
            if (maxPlayers == null) maxPlayers = 12;
        }
    }

    public record JoinLobbyRequest(
            String password
    ) {}

    public record PlayerInfo(
            Long userId,
            String username,
            boolean isHost,
            boolean isReady
    ) {}

    public record LobbyDetailResponse(
            Long lobbyId,
            String name,
            String hostname,
            int maxPlayers,
            int currentPlayers,
            boolean hasPassword,
            boolean isLocked,
            boolean publicLobby,
            List<PlayerInfo> players,
            LocalDateTime createdAt
    ) {}
}
