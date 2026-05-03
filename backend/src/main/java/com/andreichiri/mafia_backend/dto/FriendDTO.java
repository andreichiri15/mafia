package com.andreichiri.mafia_backend.dto;

import java.time.LocalDateTime;

public class FriendDTO {

    public enum PresenceStatus {
        ONLINE,
        IN_LOBBY,
        IN_GAME,
        OFFLINE
    }

    public record FriendInfo(
            Long userId,
            String username,
            PresenceStatus status
    ) {}

    public record FriendRequestInfo(
            Long requestId,
            Long fromUserId,
            String fromUsername,
            LocalDateTime createdAt
    ) {}

    public record SendRequestBody(String username) {}

    public record UserSearchResult(
            Long userId,
            String username,
            String relationship // NONE, PENDING_OUT, PENDING_IN, FRIENDS, SELF
    ) {}
}
