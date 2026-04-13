package com.andreichiri.mafia_backend.controller;

import com.andreichiri.mafia_backend.security.UserPrincipal;
import com.andreichiri.mafia_backend.service.LobbyNotificationService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Map;

@Controller
public class LobbyWebSocketController {

    private final LobbyNotificationService notificationService;

    public LobbyWebSocketController(LobbyNotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @MessageMapping("/lobby/{lobbyId}/chat")
    public void handleChatMessage(
            @DestinationVariable Long lobbyId,
            @Payload Map<String, String> payload,
            Principal principal
    ) {
        UserPrincipal user = extractUser(principal);
        Map<String, Object> chatMessage = Map.of(
                "id", System.currentTimeMillis(),
                "player", user.username(),
                "message", payload.getOrDefault("message", ""),
                "timestamp", LocalDateTime.now().toString()
        );
        notificationService.broadcastChatMessage(lobbyId, chatMessage);
    }

    private UserPrincipal extractUser(Principal principal) {
        if (principal instanceof UsernamePasswordAuthenticationToken auth) {
            return (UserPrincipal) auth.getPrincipal();
        }
        throw new RuntimeException("Unauthenticated WebSocket connection");
    }
}
