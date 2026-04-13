package com.andreichiri.mafia_backend.repositories;

import com.andreichiri.mafia_backend.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByLobbyIdAndChatChannel(Long lobbyId, Message.ChatChannel chatChannel);
    List<Message> findByLobbyIdOrderBySentAtAsc(Long lobbyId);
    List<Message> findByGameIdAndChatChannel(Long gameId, Message.ChatChannel chatChannel);
}
