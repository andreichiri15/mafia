package com.andreichiri.mafia_backend.controller;

import com.andreichiri.mafia_backend.entity.GameAction;
import com.andreichiri.mafia_backend.security.UserPrincipal;
import com.andreichiri.mafia_backend.service.GameChatService;
import com.andreichiri.mafia_backend.service.GameService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;

@Controller
public class GameWebSocketController {

    private final GameService gameService;
    private final GameChatService gameChatService;

    public GameWebSocketController(GameService gameService, GameChatService gameChatService) {
        this.gameService = gameService;
        this.gameChatService = gameChatService;
    }

    @MessageMapping("/game/{gameId}/action")
    public void handleAction(
            @DestinationVariable Long gameId,
            @Payload Map<String, String> payload,
            Principal principal
    ) {
        UserPrincipal user = extractUser(principal);
        Long targetUserId = Long.parseLong(payload.get("targetUserId"));
        GameAction.ActionType actionType = GameAction.ActionType.valueOf(payload.get("actionType"));
        gameService.submitAction(gameId, user.userId(), actionType, targetUserId);
    }

    @MessageMapping("/game/{gameId}/chat/{channel}")
    public void handleGameChat(
            @DestinationVariable Long gameId,
            @DestinationVariable String channel,
            @Payload Map<String, String> payload,
            Principal principal
    ) {
        UserPrincipal user = extractUser(principal);
        String content = payload.getOrDefault("message", "");
        gameChatService.sendGameChat(gameId, user.userId(), content, channel);
    }

    private UserPrincipal extractUser(Principal principal) {
        if (principal instanceof UsernamePasswordAuthenticationToken auth) {
            return (UserPrincipal) auth.getPrincipal();
        }
        throw new RuntimeException("Unauthenticated WebSocket connection");
    }
}
