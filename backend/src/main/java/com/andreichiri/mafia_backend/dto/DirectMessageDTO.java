package com.andreichiri.mafia_backend.dto;

import java.time.LocalDateTime;

public class DirectMessageDTO {

    public record DirectMessageInfo(
            Long id,
            Long senderId,
            String senderUsername,
            Long receiverId,
            String content,
            LocalDateTime sentAt
    ) {}

    public record SendDirectMessageBody(
            Long receiverId,
            String content
    ) {}
}
