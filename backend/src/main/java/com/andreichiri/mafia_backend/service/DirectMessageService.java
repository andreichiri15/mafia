package com.andreichiri.mafia_backend.service;

import com.andreichiri.mafia_backend.dto.DirectMessageDTO;
import com.andreichiri.mafia_backend.entity.DirectMessage;
import com.andreichiri.mafia_backend.entity.Friendship;
import com.andreichiri.mafia_backend.entity.MafiaUser;
import com.andreichiri.mafia_backend.repositories.DirectMessageRepository;
import com.andreichiri.mafia_backend.repositories.FriendshipRepository;
import com.andreichiri.mafia_backend.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DirectMessageService {

    @Autowired
    private DirectMessageRepository directMessageRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private FriendshipRepository friendshipRepository;
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Transactional(readOnly = true)
    public List<DirectMessageDTO.DirectMessageInfo> getConversation(Long userA, Long userB) {
        requireFriends(userA, userB);
        return directMessageRepository.findConversation(userA, userB).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public DirectMessageDTO.DirectMessageInfo sendMessage(Long senderId, Long receiverId, String content) {
        if (content == null || content.isBlank()) {
            throw new RuntimeException("Empty message");
        }
        if (senderId.equals(receiverId)) {
            throw new RuntimeException("Cannot send messages to yourself");
        }
        requireFriends(senderId, receiverId);

        MafiaUser sender = userRepository.findById(senderId)
                .orElseThrow(() -> new RuntimeException("Sender not found"));
        MafiaUser receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new RuntimeException("Receiver not found"));

        DirectMessage msg = new DirectMessage();
        msg.setSender(sender);
        msg.setReceiver(receiver);
        msg.setContent(content.length() > 1000 ? content.substring(0, 1000) : content);
        msg.setSentAt(LocalDateTime.now());
        directMessageRepository.save(msg);

        DirectMessageDTO.DirectMessageInfo info = toDto(msg);

        // Broadcast to both endpoints — sender's other tabs and the receiver
        messagingTemplate.convertAndSend("/topic/user/" + receiverId + "/dm", info);
        messagingTemplate.convertAndSend("/topic/user/" + senderId + "/dm", info);

        return info;
    }

    private void requireFriends(Long userA, Long userB) {
        Friendship f = friendshipRepository.findBetween(userA, userB)
                .orElseThrow(() -> new RuntimeException("You are not friends with this user"));
        if (f.getStatus() != Friendship.Status.ACCEPTED) {
            throw new RuntimeException("You are not friends with this user");
        }
    }

    private DirectMessageDTO.DirectMessageInfo toDto(DirectMessage m) {
        return new DirectMessageDTO.DirectMessageInfo(
                m.getId(),
                m.getSender().getUserId(),
                m.getSender().getUsername(),
                m.getReceiver().getUserId(),
                m.getContent(),
                m.getSentAt()
        );
    }
}
