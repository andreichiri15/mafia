package com.andreichiri.mafia_backend.listener;

import com.andreichiri.mafia_backend.entity.Game;
import com.andreichiri.mafia_backend.entity.GamePlayer;
import com.andreichiri.mafia_backend.entity.Lobby;
import com.andreichiri.mafia_backend.entity.MafiaUser;
import com.andreichiri.mafia_backend.repositories.GamePlayerRepository;
import com.andreichiri.mafia_backend.repositories.LobbyRepository;
import com.andreichiri.mafia_backend.repositories.UserRepository;
import com.andreichiri.mafia_backend.service.EloService;
import com.andreichiri.mafia_backend.service.MatchmakingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Watches WebSocket disconnects. If a player drops mid-ranked-game and stays
 * disconnected past the grace period, they take an ELO penalty and a re-queue cooldown.
 * If a lobby host drops from a lobby that hasn't started a game yet, the lobby
 * is torn down so it doesn't linger as a ghost lobby.
 *
 * A short grace (10s) means a quick refresh / network blip doesn't trigger either action.
 */
@Component
public class RankedQuitListener {

    private static final long DISCONNECT_GRACE_MS = 10_000;
    private static final long COOLDOWN_MINUTES = 15;

    @Autowired
    private SimpUserRegistry userRegistry;
    @Autowired
    private GamePlayerRepository gamePlayerRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private EloService eloService;
    @Autowired
    private MatchmakingService matchmakingService;
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    @Autowired
    private LobbyRepository lobbyRepository;
    // Self-injected so @Transactional methods below run through the Spring
    // proxy — otherwise self-calls from processPendingDisconnects would
    // bypass the proxy and lazy JPA collections would fail (OSIV is off).
    @Autowired
    @Lazy
    private RankedQuitListener self;

    /** When each pending disconnect was first noticed. */
    private final ConcurrentHashMap<Long, Long> disconnectPending = new ConcurrentHashMap<>();

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        if (event.getUser() == null) return;
        Long userId = parseUserId(event.getUser().getName());
        if (userId == null) return;

        matchmakingService.dequeue(userId);

        disconnectPending.putIfAbsent(userId, System.currentTimeMillis());
    }

    @Scheduled(fixedDelay = 5000)
    public void processPendingDisconnects() {
        long now = System.currentTimeMillis();
        Set<Long> ready = new HashSet<>();
        disconnectPending.forEach((userId, since) -> {
            if (now - since >= DISCONNECT_GRACE_MS) ready.add(userId);
        });
        for (Long userId : ready) {
            disconnectPending.remove(userId);
            if (userRegistry.getUser(userId.toString()) != null) continue;
            self.applyPenaltyIfInRankedGame(userId);
            self.deleteIdleHostedLobbies(userId);
        }
    }

    /**
     * A lobby whose host has been disconnected past the grace period AND
     * which never entered a game is considered abandoned. Delete it (and
     * clean up its bots, which are safe to remove since no game references them).
     * If a game has started, we leave the lobby alone — the game keeps running
     * without the host, and endGame will delete the lobby when the game finishes.
     */
    @Transactional
    public void deleteIdleHostedLobbies(Long userId) {
        List<Lobby> lobbies = lobbyRepository.findByHostUserIdAndGameIsNull(userId);
        for (Lobby lobby : lobbies) {
            Long lobbyId = lobby.getId();

            List<Long> botUserIds = lobby.getLobbyPlayers().stream()
                    .filter(lp -> lp.getUser() != null && Boolean.TRUE.equals(lp.getUser().getIsBot()))
                    .map(lp -> lp.getUser().getUserId())
                    .collect(Collectors.toList());

            messagingTemplate.convertAndSend(
                    "/topic/lobby/" + lobbyId + "/closed",
                    Map.of("reason", "HOST_DISCONNECTED")
            );
            lobbyRepository.delete(lobby);
            if (!botUserIds.isEmpty()) {
                userRepository.deleteAllById(botUserIds);
            }
        }
    }

    @Transactional
    public void applyPenaltyIfInRankedGame(Long userId) {
        // Is this user currently in an active ranked game?
        GamePlayer gp = gamePlayerRepository.findAll().stream()
                .filter(p -> p.getUser() != null && userId.equals(p.getUser().getUserId()))
                .filter(p -> p.getGame() != null
                        && Boolean.TRUE.equals(p.getGame().getRanked())
                        && p.getGame().getGamePhase() != Game.GamePhase.GAME_OVER
                        && Boolean.TRUE.equals(p.getAlive()))
                .findFirst()
                .orElse(null);
        if (gp == null) return;

        MafiaUser user = gp.getUser();
        int oldElo = user.getElo();
        int penalty = EloService.QUIT_PENALTY;
        int newElo = Math.max(0, oldElo - penalty);
        user.setElo(newElo);
        user.setRankedCooldownUntil(LocalDateTime.now().plusMinutes(COOLDOWN_MINUTES));
        userRepository.save(user);

        // Mark them dead so the game can continue
        gp.setAlive(false);
        gp.setKilledAtRound(gp.getGame().getCurrentRound());
        gp.setDeathCause(GamePlayer.DeathCause.DISCONNECTED);
        gamePlayerRepository.save(gp);

        messagingTemplate.convertAndSend(
                "/topic/user/" + userId + "/elo-update",
                java.util.Map.of(
                        "gameId", gp.getGame().getId(),
                        "delta", -penalty,
                        "newElo", newElo,
                        "reason", "QUIT"
                )
        );
        // Suppress unused-import warnings via reference
        if (eloService == null) { /* compile-time reference */ }
    }

    private Long parseUserId(String name) {
        try {
            return Long.parseLong(name);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
