package com.andreichiri.mafia_backend.dto;

public class LiveKitDTO {

    /**
     * Scope of the requested voice room. The backend decides the actual room
     * name and the permission grants based on the scope and the user's state.
     */
    public enum Scope {
        LOBBY,
        GAME_MAIN,
        GAME_MAFIA
    }

    public record TokenRequest(
            Scope scope,
            Long id
    ) {}

    public record TokenResponse(
            String token,
            String url,
            String roomName,
            boolean canPublish
    ) {}
}
