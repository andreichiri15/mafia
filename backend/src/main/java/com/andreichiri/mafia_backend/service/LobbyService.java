package com.andreichiri.mafia_backend.service;

import com.andreichiri.mafia_backend.dto.LobbyDTO;
import com.andreichiri.mafia_backend.dto.LobbySummaryReport;
import com.andreichiri.mafia_backend.entity.Lobby;
import com.andreichiri.mafia_backend.entity.LobbyPlayer;
import com.andreichiri.mafia_backend.entity.MafiaUser;
import com.andreichiri.mafia_backend.repositories.LobbyPlayerRepository;
import com.andreichiri.mafia_backend.repositories.LobbyRepository;
import com.andreichiri.mafia_backend.repositories.UserRepository;
import com.andreichiri.mafia_backend.security.UserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LobbyService {
    @Autowired
    private LobbyRepository lobbyRepository;
    @Autowired
    private LobbyPlayerRepository lobbyPlayerRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private LobbyNotificationService notificationService;

    public List<LobbySummaryReport> searchPublicLobbies(String searchName) {
        List<Lobby> lobbies;
        if (searchName != null && !searchName.isBlank()) {
            lobbies = lobbyRepository.getLobbiesByNameContainingIgnoreCaseAndPublicLobby(searchName, true);
        } else {
            lobbies = lobbyRepository.getLobbiesByPublicLobby(true);
        }
        return lobbies.stream()
                .map(this::toLobbySummary)
                .collect(Collectors.toList());
    }

    @Transactional
    public LobbyDTO.LobbyDetailResponse createLobby(LobbyDTO.CreateLobbyRequest request) {
        UserPrincipal principal = getAuthenticatedUser();
        MafiaUser host = userRepository.findById(principal.userId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Lobby lobby = new Lobby();
        lobby.setName(request.name());
        lobby.setMaxPlayers(request.maxPlayers());
        lobby.setPassword(request.password());
        lobby.setLocked(request.isLocked());
        lobby.setPublicLobby(request.publicLobby());
        lobby.setHost(host);
        lobby.setCreatedAt(LocalDateTime.now());

        lobbyRepository.save(lobby);

        // Add the host as a lobby player
        LobbyPlayer hostPlayer = new LobbyPlayer();
        hostPlayer.setLobby(lobby);
        hostPlayer.setUser(host);
        hostPlayer.setReady(false);
        hostPlayer.setJoinedAt(LocalDateTime.now());
        lobbyPlayerRepository.save(hostPlayer);

        lobby.getLobbyPlayers().add(hostPlayer);

        return toLobbyDetail(lobby);
    }

    public LobbyDTO.LobbyDetailResponse getLobbyDetail(Long lobbyId) {
        Lobby lobby = lobbyRepository.findById(lobbyId)
                .orElse(null);
        if (lobby == null) return null;
        return toLobbyDetail(lobby);
    }

    @Transactional
    public ResponseEntity<?> joinLobby(Long lobbyId, String lobbyPassword) {
        UserPrincipal principal = getAuthenticatedUser();

        Lobby lobby = lobbyRepository.findById(lobbyId).orElse(null);
        if (lobby == null) {
            return ResponseEntity.status(404).body("Lobby not found");
        }

        if (lobby.isLocked()) {
            return ResponseEntity.status(403).body("Lobby is locked");
        }

        if (lobby.getLobbyPlayers().size() >= lobby.getMaxPlayers()) {
            return ResponseEntity.status(400).body("Lobby is full");
        }

        if (lobbyPlayerRepository.existsByLobbyIdAndUserUserId(lobbyId, principal.userId())) {
            return ResponseEntity.status(400).body("Already in this lobby");
        }

        if (lobby.getPassword() != null && !lobby.getPassword().isEmpty()) {
            if (lobbyPassword == null || !lobby.getPassword().equals(lobbyPassword)) {
                return ResponseEntity.status(401).body("Incorrect password");
            }
        }

        MafiaUser user = userRepository.findById(principal.userId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        LobbyPlayer lobbyPlayer = new LobbyPlayer();
        lobbyPlayer.setLobby(lobby);
        lobbyPlayer.setUser(user);
        lobbyPlayer.setReady(false);
        lobbyPlayer.setJoinedAt(LocalDateTime.now());
        lobbyPlayerRepository.save(lobbyPlayer);

        lobby.getLobbyPlayers().add(lobbyPlayer);

        LobbyDTO.LobbyDetailResponse detail = toLobbyDetail(lobby);
        notificationService.broadcastPlayerList(lobbyId, detail);
        notificationService.broadcastSystemMessage(lobbyId, user.getUsername() + " joined the lobby");

        return ResponseEntity.ok(detail);
    }

    @Transactional
    public ResponseEntity<?> leaveLobby(Long lobbyId) {
        UserPrincipal principal = getAuthenticatedUser();

        Lobby lobby = lobbyRepository.findById(lobbyId).orElse(null);
        if (lobby == null) {
            return ResponseEntity.status(404).body("Lobby not found");
        }

        // If the host leaves, delete the lobby
        if (lobby.getHost().getUserId().equals(principal.userId())) {
            notificationService.broadcastSystemMessage(lobbyId, "Host left. Lobby closed.");
            lobbyRepository.delete(lobby);
            return ResponseEntity.ok("Lobby deleted");
        }

        LobbyPlayer lobbyPlayer = lobbyPlayerRepository
                .findByLobbyIdAndUserUserId(lobbyId, principal.userId())
                .orElse(null);
        if (lobbyPlayer == null) {
            return ResponseEntity.status(400).body("Not in this lobby");
        }

        lobby.getLobbyPlayers().remove(lobbyPlayer);
        lobbyPlayerRepository.delete(lobbyPlayer);

        LobbyDTO.LobbyDetailResponse detail = toLobbyDetail(lobby);
        notificationService.broadcastPlayerList(lobbyId, detail);
        notificationService.broadcastSystemMessage(lobbyId, lobbyPlayer.getUser().getUsername() + " left the lobby");

        return ResponseEntity.ok(detail);
    }

    @Transactional
    public ResponseEntity<?> toggleReady(Long lobbyId) {
        UserPrincipal principal = getAuthenticatedUser();

        LobbyPlayer lobbyPlayer = lobbyPlayerRepository
                .findByLobbyIdAndUserUserId(lobbyId, principal.userId())
                .orElse(null);
        if (lobbyPlayer == null) {
            return ResponseEntity.status(400).body("Not in this lobby");
        }

        lobbyPlayer.setReady(!lobbyPlayer.isReady());
        lobbyPlayerRepository.save(lobbyPlayer);

        Lobby lobby = lobbyRepository.findById(lobbyId)
                .orElseThrow(() -> new RuntimeException("Lobby not found"));

        LobbyDTO.LobbyDetailResponse detail = toLobbyDetail(lobby);
        notificationService.broadcastPlayerList(lobbyId, detail);

        return ResponseEntity.ok(detail);
    }

    private LobbyDTO.LobbyDetailResponse toLobbyDetail(Lobby lobby) {
        List<LobbyDTO.PlayerInfo> players = lobby.getLobbyPlayers().stream()
                .map(lp -> new LobbyDTO.PlayerInfo(
                        lp.getUser().getUserId(),
                        lp.getUser().getUsername(),
                        lp.getUser().getUserId().equals(lobby.getHost().getUserId()),
                        lp.isReady()
                ))
                .collect(Collectors.toList());

        return new LobbyDTO.LobbyDetailResponse(
                lobby.getId(),
                lobby.getName(),
                lobby.getHost().getUsername(),
                lobby.getMaxPlayers(),
                lobby.getLobbyPlayers().size(),
                lobby.getPassword() != null && !lobby.getPassword().isEmpty(),
                lobby.isLocked(),
                lobby.isPublicLobby(),
                players,
                lobby.getCreatedAt()
        );
    }

    private LobbySummaryReport toLobbySummary(Lobby lobby) {
        return new LobbySummaryReport(
                lobby.getId(),
                lobby.getName(),
                lobby.getHost().getUsername(),
                lobby.getMaxPlayers(),
                lobby.getLobbyPlayers().size(),
                lobby.getCreatedAt()
        );
    }

    private UserPrincipal getAuthenticatedUser() {
        return (UserPrincipal) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
    }
}
