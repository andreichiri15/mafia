package com.andreichiri.mafia_backend.listener;

import com.andreichiri.mafia_backend.entity.Game;
import com.andreichiri.mafia_backend.entity.GamePlayer;
import com.andreichiri.mafia_backend.entity.MafiaUser;
import com.andreichiri.mafia_backend.repositories.GamePlayerRepository;
import com.andreichiri.mafia_backend.repositories.UserRepository;
import com.andreichiri.mafia_backend.service.EloService;
import com.andreichiri.mafia_backend.service.MatchmakingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Watches WebSocket disconnects. If a player drops mid-ranked-game and stays
 * disconnected past the grace period, they take an ELO penalty and a re-queue cooldown.
 *
 * A short grace (10s) means a quick refresh / network blip doesn't trigger the penalty.
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

    /** When each pending disconnect was first noticed. */
    private final ConcurrentHashMap<Long, Long> disconnectPending = new ConcurrentHashMap<>();

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        if (event.getUser() == null) return;
        Long userId = parseUserId(event.getUser().getName());
        if (userId == null) return;
        // Always evict from queue immediately
        matchmakingService.dequeue(userId);
        // Mark pending — the scheduled check will confirm after the grace window
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
            // If the user reconnected (now back in registry), no penalty
            if (userRegistry.getUser(userId.toString()) != null) continue;
            applyPenaltyIfInRankedGame(userId);
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
