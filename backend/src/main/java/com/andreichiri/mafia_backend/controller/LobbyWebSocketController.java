package com.andreichiri.mafia_backend.controller;

import com.andreichiri.mafia_backend.security.UserPrincipal;
import com.andreichiri.mafia_backend.service.LobbyChatService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;

@Controller
public class LobbyWebSocketController {

    private final LobbyChatService lobbyChatService;

    public LobbyWebSocketController(LobbyChatService lobbyChatService) {
        this.lobbyChatService = lobbyChatService;
    }

    @MessageMapping("/lobby/{lobbyId}/chat")
    public void handleChatMessage(
            @DestinationVariable Long lobbyId,
            @Payload Map<String, String> payload,
            Principal principal
    ) {
        UserPrincipal user = extractUser(principal);
        String content = payload.getOrDefault("message", "");
        lobbyChatService.sendMessage(lobbyId, user.userId(), content);
    }

    private UserPrincipal extractUser(Principal principal) {
        if (principal instanceof UsernamePasswordAuthenticationToken auth) {
            return (UserPrincipal) auth.getPrincipal();
        }
        throw new RuntimeException("Unauthenticated WebSocket connection");
    }
}
