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
    @Autowired
    private PhaseTimerService phaseTimerService;

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

        // If the host leaves, delete the lobby AND every piece of data associated with it
        if (lobby.getHost().getUserId().equals(principal.userId())) {
            // Cancel any running phase timer before the game gets cascade-deleted,
            // otherwise the scheduled callback would fire against a deleted game
            if (lobby.getGame() != null) {
                phaseTimerService.cancelTimer(lobby.getGame().getId());
            }

            // Collect bot user IDs so we can clean them up after the lobby is deleted
            List<Long> botUserIds = lobby.getLobbyPlayers().stream()
                    .filter(lp -> lp.getUser() != null && lp.getUser().getIsBot())
                    .map(lp -> lp.getUser().getUserId())
                    .collect(Collectors.toList());

            notificationService.broadcastSystemMessage(lobbyId, "Host left. Lobby closed.");
            messagingTemplate.convertAndSend(
                    "/topic/lobby/" + lobbyId + "/closed",
                    Map.of("reason", "HOST_LEFT")
            );
            if (lobby.getGame() != null) {
                messagingTemplate.convertAndSend(
                        "/topic/game/" + lobby.getGame().getId() + "/closed",
                        Map.of("reason", "HOST_LEFT")
                );
            }

            // Cascade-deletes: lobbyPlayers, game (which cascades to gamePlayers,
            // gameActions, game messages), lobby messages
            lobbyRepository.delete(lobby);

            // Bots are throwaway MafiaUser rows — delete them so they don't pile up
            if (!botUserIds.isEmpty()) {
                userRepository.deleteAllById(botUserIds);
            }
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
    public ResponseEntity<?> addBot(Long lobbyId) {
        UserPrincipal principal = getAuthenticatedUser();
        Lobby lobby = lobbyRepository.findById(lobbyId).orElse(null);
        if (lobby == null) return ResponseEntity.status(404).body("Lobby not found");
        if (!lobby.getHost().getUserId().equals(principal.userId())) {
            return ResponseEntity.status(403).body("Only the host can add bots");
        }
        if (lobby.isLocked()) {
            return ResponseEntity.status(400).body("Cannot add bots once the game has started");
        }
        if (lobby.getLobbyPlayers().size() >= lobby.getMaxPlayers()) {
            return ResponseEntity.status(400).body("Lobby is full");
        }

        // Create the bot user with a unique readable username
        MafiaUser bot = new MafiaUser();
        bot.setUsername(generateUniqueBotUsername());
        bot.setEmail("bot-" + UUID.randomUUID() + "@bot.local");
        bot.setPassword(UUID.randomUUID().toString()); // not bcrypt-encoded, so login can never match
        bot.setIsBot(true);
        userRepository.save(bot);

        LobbyPlayer lp = new LobbyPlayer();
        lp.setLobby(lobby);
        lp.setUser(bot);
        lp.setReady(true); // bots are always ready
        lp.setJoinedAt(LocalDateTime.now());
        lobbyPlayerRepository.save(lp);
        lobby.getLobbyPlayers().add(lp);

        LobbyDTO.LobbyDetailResponse detail = toLobbyDetail(lobby);
        notificationService.broadcastPlayerList(lobbyId, detail);
        notificationService.broadcastSystemMessage(lobbyId, bot.getUsername() + " was added to the lobby");
        return ResponseEntity.ok(detail);
    }

    @Transactional
    public ResponseEntity<?> removeBot(Long lobbyId, Long botUserId) {
        UserPrincipal principal = getAuthenticatedUser();
        Lobby lobby = lobbyRepository.findById(lobbyId).orElse(null);
        if (lobby == null) return ResponseEntity.status(404).body("Lobby not found");
        if (!lobby.getHost().getUserId().equals(principal.userId())) {
            return ResponseEntity.status(403).body("Only the host can remove bots");
        }
        if (lobby.isLocked()) {
            return ResponseEntity.status(400).body("Cannot remove bots once the game has started");
        }

        LobbyPlayer lp = lobbyPlayerRepository
                .findByLobbyIdAndUserUserId(lobbyId, botUserId)
                .orElse(null);
        if (lp == null) return ResponseEntity.status(404).body("Bot not in this lobby");
        if (lp.getUser() == null || !lp.getUser().getIsBot()) {
            return ResponseEntity.status(400).body("Target is not a bot");
        }

        String botName = lp.getUser().getUsername();
        lobby.getLobbyPlayers().remove(lp);
        lobbyPlayerRepository.delete(lp);
        userRepository.deleteById(botUserId);

        LobbyDTO.LobbyDetailResponse detail = toLobbyDetail(lobby);
        notificationService.broadcastPlayerList(lobbyId, detail);
        notificationService.broadcastSystemMessage(lobbyId, botName + " was removed from the lobby");
        return ResponseEntity.ok(detail);
    }

    private String generateUniqueBotUsername() {
        for (int i = 0; i < 20; i++) {
            String suffix = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
            String candidate = "Bot-" + suffix;
            if (!userRepository.existsByUsername(candidate)) return candidate;
        }
        // Vanishingly unlikely fallback
        return "Bot-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
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

        // Phase durations: clamp to [5, 600] seconds. Defaults preserve old hardcoded values.
        int night = clampDuration(settings.nightDurationSeconds(), 30);
        int day = clampDuration(settings.dayDurationSeconds(), 90);
        int voting = clampDuration(settings.votingDurationSeconds(), 30);

        lobby.setMafiaCount(mafiaCount);
        lobby.setIncludeSheriff(settings.includeSheriff());
        lobby.setIncludeDoctor(settings.includeDoctor());
        lobby.setIncludeJester(settings.includeJester());
        lobby.setIncludeMutilator(settings.includeMutilator());
        lobby.setDoctorSelfSaveLimit(selfSaveLimit);
        lobby.setSheriffInvestigationDelay(invDelay);
        lobby.setNightDurationSeconds(night);
        lobby.setDayDurationSeconds(day);
        lobby.setVotingDurationSeconds(voting);
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
                        lp.isReady(),
                        lp.getUser().getIsBot()
                ))
                .collect(Collectors.toList());

        LobbyDTO.GameSettings settings = new LobbyDTO.GameSettings(
                lobby.getMafiaCount(),
                lobby.isIncludeSheriff(),
                lobby.isIncludeDoctor(),
                lobby.isIncludeJester(),
                lobby.isIncludeMutilator(),
                lobby.getDoctorSelfSaveLimit(),
                lobby.getSheriffInvestigationDelay(),
                lobby.getNightDurationSeconds(),
                lobby.getDayDurationSeconds(),
                lobby.getVotingDurationSeconds()
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

    private int clampDuration(Integer value, int fallback) {
        int v = value != null ? value : fallback;
        if (v < 5) v = 5;
        if (v > 600) v = 600;
        return v;
    }
}
