package com.andreichiri.mafia_backend.service;

import com.andreichiri.mafia_backend.dto.LobbyDTO;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class LobbyNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    public LobbyNotificationService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void broadcastPlayerList(Long lobbyId, LobbyDTO.LobbyDetailResponse lobbyDetail) {
        messagingTemplate.convertAndSend(
                "/topic/lobby/" + lobbyId + "/players",
                lobbyDetail
        );
    }

    public void broadcastSystemMessage(Long lobbyId, String content) {
        Map<String, Object> systemMessage = Map.of(
                "id", System.currentTimeMillis(),
                "player", "System",
                "message", content,
                "timestamp", LocalDateTime.now().toString()
        );
        messagingTemplate.convertAndSend(
                "/topic/lobby/" + lobbyId + "/chat",
                systemMessage
        );
    }

    public void broadcastChatMessage(Long lobbyId, Map<String, Object> chatMessage) {
        messagingTemplate.convertAndSend(
                "/topic/lobby/" + lobbyId + "/chat",
                chatMessage
        );
    }
}
