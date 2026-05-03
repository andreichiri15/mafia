package com.andreichiri.mafia_backend.service;

import com.andreichiri.mafia_backend.dto.FriendDTO;
import com.andreichiri.mafia_backend.entity.Friendship;
import com.andreichiri.mafia_backend.entity.MafiaUser;
import com.andreichiri.mafia_backend.repositories.FriendshipRepository;
import com.andreichiri.mafia_backend.repositories.GamePlayerRepository;
import com.andreichiri.mafia_backend.repositories.LobbyPlayerRepository;
import com.andreichiri.mafia_backend.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FriendService {

    @Autowired
    private FriendshipRepository friendshipRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private LobbyPlayerRepository lobbyPlayerRepository;
    @Autowired
    private GamePlayerRepository gamePlayerRepository;
    @Autowired
    private SimpUserRegistry userRegistry;
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Transactional(readOnly = true)
    public List<FriendDTO.FriendInfo> listFriends(Long userId) {
        return friendshipRepository.findAcceptedByUser(userId).stream()
                .map(f -> {
                    MafiaUser other = f.getRequester().getUserId().equals(userId)
                            ? f.getAddressee()
                            : f.getRequester();
                    return new FriendDTO.FriendInfo(
                            other.getUserId(),
                            other.getUsername(),
                            getPresenceStatus(other.getUserId())
                    );
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<FriendDTO.FriendRequestInfo> listIncomingRequests(Long userId) {
        return friendshipRepository.findIncomingPending(userId).stream()
                .map(f -> new FriendDTO.FriendRequestInfo(
                        f.getId(),
                        f.getRequester().getUserId(),
                        f.getRequester().getUsername(),
                        f.getCreatedAt()
                ))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<FriendDTO.UserSearchResult> searchUsers(String query, Long currentUserId) {
        if (query == null || query.isBlank()) return List.of();
        List<MafiaUser> users = userRepository.findTop10ByUsernameContainingIgnoreCaseOrderByUsernameAsc(query);
        return users.stream()
                .map(u -> new FriendDTO.UserSearchResult(
                        u.getUserId(),
                        u.getUsername(),
                        determineRelationship(currentUserId, u.getUserId())
                ))
                .collect(Collectors.toList());
    }

    @Transactional
    public void sendRequest(Long requesterId, String targetUsername) {
        MafiaUser requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        MafiaUser addressee = userRepository.findByUsername(targetUsername)
                .orElseThrow(() -> new RuntimeException("User '" + targetUsername + "' not found"));

        if (addressee.getUserId().equals(requesterId)) {
            throw new RuntimeException("Cannot send a friend request to yourself");
        }

        friendshipRepository.findBetween(requesterId, addressee.getUserId()).ifPresent(f -> {
            throw new RuntimeException(
                    f.getStatus() == Friendship.Status.ACCEPTED
                            ? "You are already friends"
                            : "A friend request between you already exists"
            );
        });

        Friendship friendship = new Friendship();
        friendship.setRequester(requester);
        friendship.setAddressee(addressee);
        friendship.setStatus(Friendship.Status.PENDING);
        friendship.setCreatedAt(LocalDateTime.now());
        friendshipRepository.save(friendship);

        // Notify addressee in real-time so their UI updates
        notifyFriendRefresh(addressee.getUserId());
    }

    @Transactional
    public void acceptRequest(Long currentUserId, Long requestId) {
        Friendship friendship = friendshipRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        if (!friendship.getAddressee().getUserId().equals(currentUserId)) {
            throw new RuntimeException("Not authorized to accept this request");
        }
        if (friendship.getStatus() != Friendship.Status.PENDING) {
            throw new RuntimeException("Request is not pending");
        }

        friendship.setStatus(Friendship.Status.ACCEPTED);
        friendship.setAcceptedAt(LocalDateTime.now());
        friendshipRepository.save(friendship);

        // Notify both sides
        notifyFriendRefresh(friendship.getRequester().getUserId());
        notifyFriendRefresh(friendship.getAddressee().getUserId());
    }

    @Transactional
    public void declineRequest(Long currentUserId, Long requestId) {
        Friendship friendship = friendshipRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        if (!friendship.getAddressee().getUserId().equals(currentUserId)) {
            throw new RuntimeException("Not authorized to decline this request");
        }

        friendshipRepository.delete(friendship);
        notifyFriendRefresh(friendship.getRequester().getUserId());
        notifyFriendRefresh(friendship.getAddressee().getUserId());
    }

    @Transactional
    public void removeFriend(Long currentUserId, Long friendUserId) {
        Friendship friendship = friendshipRepository.findBetween(currentUserId, friendUserId)
                .orElseThrow(() -> new RuntimeException("Not friends"));

        friendshipRepository.delete(friendship);
        notifyFriendRefresh(currentUserId);
        notifyFriendRefresh(friendUserId);
    }

    public FriendDTO.PresenceStatus getPresenceStatus(Long userId) {
        if (gamePlayerRepository.existsActiveByUserId(userId)) {
            return FriendDTO.PresenceStatus.IN_GAME;
        }
        if (lobbyPlayerRepository.existsByUserUserId(userId)) {
            return FriendDTO.PresenceStatus.IN_LOBBY;
        }
        if (userRegistry.getUser(userId.toString()) != null) {
            return FriendDTO.PresenceStatus.ONLINE;
        }
        return FriendDTO.PresenceStatus.OFFLINE;
    }

    private String determineRelationship(Long currentUserId, Long otherUserId) {
        if (currentUserId.equals(otherUserId)) return "SELF";
        return friendshipRepository.findBetween(currentUserId, otherUserId)
                .map(f -> {
                    if (f.getStatus() == Friendship.Status.ACCEPTED) return "FRIENDS";
                    if (f.getRequester().getUserId().equals(currentUserId)) return "PENDING_OUT";
                    return "PENDING_IN";
                })
                .orElse("NONE");
    }

    private void notifyFriendRefresh(Long userId) {
        // Tell the user's UI to refetch friends/requests
        messagingTemplate.convertAndSend(
                "/topic/user/" + userId + "/friends-update",
                java.util.Map.of("type", "REFRESH")
        );
    }
}
