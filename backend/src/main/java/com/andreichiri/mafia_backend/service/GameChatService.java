package com.andreichiri.mafia_backend.service;

import com.andreichiri.mafia_backend.entity.*;
import com.andreichiri.mafia_backend.repositories.GamePlayerRepository;
import com.andreichiri.mafia_backend.repositories.GameRepository;
import com.andreichiri.mafia_backend.repositories.MessageRepository;
import com.andreichiri.mafia_backend.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class GameChatService {

    @Autowired
    private GameRepository gameRepository;
    @Autowired
    private GamePlayerRepository gamePlayerRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private MessageRepository messageRepository;
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public void sendGameChat(Long gameId, Long userId, String content, String channel) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new RuntimeException("Game not found"));

        GamePlayer player = gamePlayerRepository.findByGameIdAndUserId(gameId, userId)
                .orElseThrow(() -> new RuntimeException("Player not in this game"));

        Message.ChatChannel chatChannel = Message.ChatChannel.valueOf(channel.toUpperCase());

        // Validate access
        validateChatAccess(game, player, chatChannel);

        // Persist the message
        MafiaUser sender = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Message message = new Message();
        message.setGame(game);
        message.setSender(sender);
        message.setContent(content);
        message.setChatChannel(chatChannel);
        message.setSentAt(LocalDateTime.now());
        messageRepository.save(message);

        // Broadcast to the appropriate topic
        Map<String, Object> chatMessage = Map.of(
                "id", message.getId(),
                "player", sender.getUsername(),
                "message", content,
                "timestamp", message.getSentAt().toString(),
                "channel", chatChannel.name()
        );

        String destination = "/topic/game/" + gameId + "/chat/" + chatChannel.name().toLowerCase();
        messagingTemplate.convertAndSend(destination, chatMessage);
    }

    private void validateChatAccess(Game game, GamePlayer player, Message.ChatChannel channel) {
        switch (channel) {
            case DAY -> {
                if (game.getGamePhase() != Game.GamePhase.DAY && game.getGamePhase() != Game.GamePhase.VOTING) {
                    throw new RuntimeException("Day chat is only available during day and voting phases");
                }
                if (!player.getAlive()) {
                    throw new RuntimeException("Dead players cannot use day chat");
                }
                if (player.getSilencedUntilRound() != null
                        && player.getSilencedUntilRound() >= game.getCurrentRound()) {
                    throw new RuntimeException("You have been silenced this round");
                }
            }
            case MAFIA -> {
                if (player.getRole() != GamePlayer.Role.MAFIA) {
                    throw new RuntimeException("Only mafia members can use mafia chat");
                }
                if (game.getGamePhase() != Game.GamePhase.NIGHT) {
                    throw new RuntimeException("Mafia chat is only available at night");
                }
            }
            case DEAD -> {
                if (player.getAlive()) {
                    throw new RuntimeException("Only dead players can use dead chat");
                }
            }
            case LOBBY -> {
                throw new RuntimeException("Lobby chat is not available during the game");
            }
        }
    }
}
