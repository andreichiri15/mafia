package com.andreichiri.mafia_backend.service;

import com.andreichiri.mafia_backend.dto.LobbyDTO;
import com.andreichiri.mafia_backend.dto.LobbySummaryReport;
import com.andreichiri.mafia_backend.entity.Friendship;
import com.andreichiri.mafia_backend.entity.Lobby;
import com.andreichiri.mafia_backend.entity.LobbyPlayer;
import com.andreichiri.mafia_backend.entity.MafiaUser;
import com.andreichiri.mafia_backend.repositories.FriendshipRepository;
import com.andreichiri.mafia_backend.repositories.LobbyPlayerRepository;
import com.andreichiri.mafia_backend.repositories.LobbyRepository;
import com.andreichiri.mafia_backend.repositories.UserRepository;
import com.andreichiri.mafia_backend.security.UserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
    @Autowired
    private FriendshipRepository friendshipRepository;
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Transactional(readOnly = true)
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
        lobby.setGeneratedLink(generateUniqueInviteToken());

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

    @Transactional(readOnly = true)
    public LobbyDTO.InviteResolution resolveInviteToken(String token) {
        Lobby lobby = lobbyRepository.findByGeneratedLink(token).orElse(null);
        if (lobby == null) return null;
        return new LobbyDTO.InviteResolution(
                lobby.getId(),
                lobby.getName(),
                lobby.getHost().getUsername(),
                lobby.getLobbyPlayers().size(),
                lobby.getMaxPlayers(),
                lobby.isLocked(),
                lobby.getGame() != null
        );
    }

    @Transactional
    public ResponseEntity<?> inviteFriend(Long lobbyId, Long friendUserId) {
        UserPrincipal principal = getAuthenticatedUser();
        Lobby lobby = lobbyRepository.findById(lobbyId).orElse(null);
        if (lobby == null) return ResponseEntity.status(404).body("Lobby not found");

        // Inviter must be in the lobby
        boolean inLobby = lobby.getLobbyPlayers().stream()
                .anyMatch(lp -> lp.getUser().getUserId().equals(principal.userId()));
        if (!inLobby) return ResponseEntity.status(403).body("You must be in the lobby to invite");

        // Target must be an accepted friend
        Friendship f = friendshipRepository.findBetween(principal.userId(), friendUserId).orElse(null);
        if (f == null || f.getStatus() != Friendship.Status.ACCEPTED) {
            return ResponseEntity.status(400).body("Not friends with this user");
        }

        MafiaUser inviter = userRepository.findById(principal.userId())
                .orElseThrow(() -> new RuntimeException("Inviter not found"));

        Map<String, Object> payload = Map.of(
                "lobbyId", lobby.getId(),
                "lobbyName", lobby.getName(),
                "inviteToken", lobby.getGeneratedLink() == null ? "" : lobby.getGeneratedLink(),
                "inviterUsername", inviter.getUsername(),
                "sentAt", LocalDateTime.now().toString()
        );
        messagingTemplate.convertAndSend("/topic/user/" + friendUserId + "/lobby-invite", payload);

        return ResponseEntity.ok().build();
    }

    private String generateUniqueInviteToken() {
        // 16-char alphanumeric token; collision risk is negligible at this scale
        for (int attempt = 0; attempt < 5; attempt++) {
            String token = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
            if (lobbyRepository.findByGeneratedLink(token).isEmpty()) return token;
        }
        // Should never happen, but if every attempt collided, fall back to full UUID
        return UUID.randomUUID().toString().replace("-", "");
    }

    @Transactional(readOnly = true)
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

    @Transactional
    public ResponseEntity<?> updateSettings(Long lobbyId, LobbyDTO.GameSettings settings) {
        UserPrincipal principal = getAuthenticatedUser();
        Lobby lobby = lobbyRepository.findById(lobbyId).orElse(null);
        if (lobby == null) return ResponseEntity.status(404).body("Lobby not found");
        if (!lobby.getHost().getUserId().equals(principal.userId())) {
            return ResponseEntity.status(403).body("Only the host can change settings");
        }
        if (lobby.isLocked()) {
            return ResponseEntity.status(400).body("Cannot change settings during a game");
        }

        // Validate
        int playerCount = lobby.getLobbyPlayers().size();
        int maxPlayers = lobby.getMaxPlayers();
        int mafiaCount = settings.mafiaCount() == null ? 1 : settings.mafiaCount();
        if (mafiaCount < 1) return ResponseEntity.badRequest().body("Mafia count must be at least 1");
        // Upper bound: less than half the max players (mafia can't be majority by start)
        int maxMafia = Math.max(1, (maxPlayers - 1) / 2);
        if (mafiaCount > maxMafia) {
            return ResponseEntity.badRequest().body("Mafia count cannot exceed " + maxMafia + " for a " + maxPlayers + "-player lobby");
        }

        int specialCount = (settings.includeSheriff() ? 1 : 0)
                + (settings.includeDoctor() ? 1 : 0)
                + (settings.includeJester() ? 1 : 0)
                + (settings.includeMutilator() ? 1 : 0);
        if (mafiaCount + specialCount > maxPlayers) {
            return ResponseEntity.badRequest().body("Too many roles selected for the player capacity");
        }

        Integer selfSaveLimit = settings.doctorSelfSaveLimit() == null ? -1 : settings.doctorSelfSaveLimit();
        if (selfSaveLimit < -1) selfSaveLimit = -1;
        Integer invDelay = settings.sheriffInvestigationDelay() == null ? 0 : settings.sheriffInvestigationDelay();
        if (invDelay < 0) invDelay = 0;

        lobby.setMafiaCount(mafiaCount);
        lobby.setIncludeSheriff(settings.includeSheriff());
        lobby.setIncludeDoctor(settings.includeDoctor());
        lobby.setIncludeJester(settings.includeJester());
        lobby.setIncludeMutilator(settings.includeMutilator());
        lobby.setDoctorSelfSaveLimit(selfSaveLimit);
        lobby.setSheriffInvestigationDelay(invDelay);
        lobbyRepository.save(lobby);

        // Reuse silent suppress for playerCount
        if (playerCount > 0) { /* no-op, just to suppress unused warning */ }

        LobbyDTO.LobbyDetailResponse detail = toLobbyDetail(lobby);
        notificationService.broadcastPlayerList(lobbyId, detail);
        notificationService.broadcastSystemMessage(lobbyId, "Game settings updated by the host");
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

        LobbyDTO.GameSettings settings = new LobbyDTO.GameSettings(
                lobby.getMafiaCount(),
                lobby.isIncludeSheriff(),
                lobby.isIncludeDoctor(),
                lobby.isIncludeJester(),
                lobby.isIncludeMutilator(),
                lobby.getDoctorSelfSaveLimit(),
                lobby.getSheriffInvestigationDelay()
        );

        return new LobbyDTO.LobbyDetailResponse(
                lobby.getId(),
                lobby.getName(),
                lobby.getHost().getUsername(),
                lobby.getMaxPlayers(),
                lobby.getLobbyPlayers().size(),
                lobby.getPassword() != null && !lobby.getPassword().isEmpty(),
                lobby.isLocked(),
                lobby.isPublicLobby(),
                lobby.getGeneratedLink(),
                players,
                settings,
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
