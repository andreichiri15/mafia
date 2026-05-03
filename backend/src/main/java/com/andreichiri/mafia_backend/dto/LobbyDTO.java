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

    public record GameSettings(
            Integer mafiaCount,
            boolean includeSheriff,
            boolean includeDoctor,
            boolean includeJester,
            boolean includeMutilator,
            Integer doctorSelfSaveLimit,
            Integer sheriffInvestigationDelay
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
            String inviteToken,
            List<PlayerInfo> players,
            GameSettings settings,
            LocalDateTime createdAt
    ) {}

    public record InviteResolution(
            Long lobbyId,
            String name,
            String hostname,
            int currentPlayers,
            int maxPlayers,
            boolean isLocked,
            boolean inGame
    ) {}
}
