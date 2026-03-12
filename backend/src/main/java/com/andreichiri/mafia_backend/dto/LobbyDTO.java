package com.andreichiri.mafia_backend.dto;

import java.time.LocalDateTime;

public class LobbyDTO {
    public record CreateLobbyRequest(
            String name,
            Integer maxPlayers,
            String password,
            boolean isLocked
    ) {
        public CreateLobbyRequest {
            if (maxPlayers == null) maxPlayers = 12;
        }
    }

    public record LobbyDetailResponse(
            Long lobbyId,
            String name,
            String hostname,
            int maxPlayers,
            int currentPlayers,
            boolean hasPassword,
            boolean isLocked,
            LocalDateTime createdAt
    ) {}
}
