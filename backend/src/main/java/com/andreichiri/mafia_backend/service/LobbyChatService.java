package com.andreichiri.mafia_backend.service;

import com.andreichiri.mafia_backend.entity.Lobby;
import com.andreichiri.mafia_backend.entity.MafiaUser;
import com.andreichiri.mafia_backend.entity.Message;
import com.andreichiri.mafia_backend.repositories.LobbyPlayerRepository;
import com.andreichiri.mafia_backend.repositories.LobbyRepository;
import com.andreichiri.mafia_backend.repositories.MessageRepository;
import com.andreichiri.mafia_backend.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class LobbyChatService {

    private static final int HISTORY_LIMIT = 100;

    @Autowired
    private LobbyRepository lobbyRepository;
    @Autowired
    private LobbyPlayerRepository lobbyPlayerRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private MessageRepository messageRepository;
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Transactional
    public void sendMessage(Long lobbyId, Long senderUserId, String content) {
        if (content == null || content.isBlank()) return;

        Lobby lobby = lobbyRepository.findById(lobbyId)
                .orElseThrow(() -> new RuntimeException("Lobby not found"));

        // Sender must be a member of this lobby
        if (!lobbyPlayerRepository.existsByLobbyIdAndUserUserId(lobbyId, senderUserId)) {
            throw new RuntimeException("Not a member of this lobby");
        }

        MafiaUser sender = userRepository.findById(senderUserId)
                .orElseThrow(() -> new RuntimeException("Sender not found"));

        Message msg = new Message();
        msg.setLobby(lobby);
        msg.setSender(sender);
        msg.setContent(content.length() > 200 ? content.substring(0, 200) : content);
        msg.setChatChannel(Message.ChatChannel.LOBBY);
        msg.setSentAt(LocalDateTime.now());
        messageRepository.save(msg);

        Map<String, Object> payload = Map.of(
                "id", msg.getId(),
                "player", sender.getUsername(),
                "message", msg.getContent(),
                "timestamp", msg.getSentAt().toString(),
                "channel", "LOBBY"
        );
        messagingTemplate.convertAndSend("/topic/lobby/" + lobbyId + "/chat", payload);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getHistory(Long lobbyId, Long requesterUserId) {
        if (!lobbyPlayerRepository.existsByLobbyIdAndUserUserId(lobbyId, requesterUserId)) {
            throw new RuntimeException("Not a member of this lobby");
        }
        List<Message> messages = messageRepository.findByLobbyIdOrderBySentAtAsc(lobbyId);
        // Cap to last HISTORY_LIMIT
        int from = Math.max(0, messages.size() - HISTORY_LIMIT);
        return messages.subList(from, messages.size()).stream()
                .map(m -> {
                    Map<String, Object> map = new java.util.HashMap<>();
                    map.put("id", m.getId());
                    map.put("player", m.getSender() != null ? m.getSender().getUsername() : "(unknown)");
                    map.put("message", m.getContent());
                    map.put("timestamp", m.getSentAt() != null ? m.getSentAt().toString() : "");
                    map.put("channel", m.getChatChannel().name());
                    return map;
                })
                .toList();
    }
}
