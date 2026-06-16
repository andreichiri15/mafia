package com.andreichiri.mafia_backend.service;

import com.andreichiri.mafia_backend.dto.RankedDTO;
import com.andreichiri.mafia_backend.entity.Lobby;
import com.andreichiri.mafia_backend.entity.LobbyPlayer;
import com.andreichiri.mafia_backend.entity.MafiaUser;
import com.andreichiri.mafia_backend.config.RankedConfig;
import com.andreichiri.mafia_backend.repositories.LobbyPlayerRepository;
import com.andreichiri.mafia_backend.repositories.LobbyRepository;
import com.andreichiri.mafia_backend.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@EnableScheduling
public class MatchmakingService {

    /** ±10% ELO band around the anchor player. */
    private static final double STRICT_BAND_RATIO = 0.10;
    /** After this long in queue, the player's ELO band becomes unbounded. */
    private static final long ELO_WAIT_LIMIT_MS = 30_000;

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private LobbyRepository lobbyRepository;
    @Autowired
    private LobbyPlayerRepository lobbyPlayerRepository;
    @Autowired
    private RankedConfigService rankedConfigService;
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    @Autowired
    @Lazy
    private GameService gameService;
    /** Self-reference through the Spring proxy so @Transactional actually applies. */
    @Autowired
    @Lazy
    private MatchmakingService self;

    private static final class Entry {
        final Long userId;
        final int elo;
        final long enqueuedAtMs;
        Entry(Long userId, int elo) {
            this.userId = userId;
            this.elo = elo;
            this.enqueuedAtMs = System.currentTimeMillis();
        }
    }

    private final Map<Long, Entry> queue = new ConcurrentHashMap<>();

    public boolean isInQueue(Long userId) {
        return queue.containsKey(userId);
    }

    public int queueSize() {
        return queue.size();
    }

    public RankedDTO.QueueStatus statusFor(Long userId) {
        MafiaUser user = userRepository.findById(userId).orElse(null);
        Entry e = queue.get(userId);
        Integer elo = user != null ? user.getElo() : null;
        Long cooldownUntilMs = user != null && user.getRankedCooldownUntil() != null
                ? user.getRankedCooldownUntil().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                : null;
        return new RankedDTO.QueueStatus(
                e != null,
                queue.size(),
                e != null ? e.enqueuedAtMs : 0L,
                elo,
                cooldownUntilMs
        );
    }

    /** Adds the user to the queue. Throws if they're under cooldown or already in a lobby/game. */
    public void enqueue(Long userId) {
        MafiaUser user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (user.getRankedCooldownUntil() != null
                && user.getRankedCooldownUntil().isAfter(LocalDateTime.now())) {
            throw new RuntimeException("You are on ranked cooldown until " + user.getRankedCooldownUntil());
        }
        if (lobbyPlayerRepository.existsByUserUserId(userId)) {
            throw new RuntimeException("Leave your current lobby/game before queueing");
        }
        queue.putIfAbsent(userId, new Entry(userId, user.getElo()));
    }

    public void dequeue(Long userId) {
        queue.remove(userId);
    }

    /** Result of a successful match — used by tick() to broadcast AFTER the transaction commits. */
    public record MatchResult(Long gameId, List<Long> userIds) {}

    /** Periodic tick — try to form a match. */
    @Scheduled(fixedDelay = 3000)
    public void tick() {
        try {
            // Call through the proxy so @Transactional applies AND we know the
            // transaction has committed before we broadcast.
            MatchResult result = self.tryFormMatch();
            if (result == null) return;
            for (Long userId : result.userIds()) {
                messagingTemplate.convertAndSend(
                        "/topic/user/" + userId + "/ranked-match",
                        new RankedDTO.MatchFoundEvent(result.gameId())
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Picks a set of players from the queue, builds the ephemeral lobby +
     * game in a single transaction, and returns the matched IDs so the
     * caller can broadcast post-commit.
     */
    @Transactional
    public MatchResult tryFormMatch() {
        RankedConfig cfg = rankedConfigService.getConfig();
        int needed = cfg.getPlayerCount();
        if (queue.size() < needed) return null;

        // Snapshot — sort by waiting time so the longest-waiter "anchors" the match
        List<Entry> all = new ArrayList<>(queue.values());
        all.sort(Comparator.comparingLong(e -> e.enqueuedAtMs));

        Entry anchor = all.get(0);
        long now = System.currentTimeMillis();
        boolean anchorExpired = (now - anchor.enqueuedAtMs) >= ELO_WAIT_LIMIT_MS;

        List<Entry> picked;
        if (anchorExpired) {
            picked = new ArrayList<>(all.subList(0, needed));
        } else {
            double lo = anchor.elo * (1 - STRICT_BAND_RATIO);
            double hi = anchor.elo * (1 + STRICT_BAND_RATIO);
            picked = new ArrayList<>();
            picked.add(anchor);
            for (Entry e : all) {
                if (e == anchor) continue;
                if (e.elo >= lo && e.elo <= hi) picked.add(e);
                if (picked.size() == needed) break;
            }
            if (picked.size() < needed) return null;
        }

        // Atomically remove from queue
        List<Long> matchedIds = new ArrayList<>();
        for (Entry e : picked) {
            if (queue.remove(e.userId) != null) matchedIds.add(e.userId);
        }
        if (matchedIds.size() < needed) {
            // Someone bailed between snapshot and removal — restore + retry next tick
            for (Long id : matchedIds) queue.putIfAbsent(id, new Entry(id, picked.stream()
                    .filter(p -> p.userId.equals(id)).findFirst().map(p -> p.elo).orElse(1000)));
            return null;
        }

        Long gameId = createRankedGame(matchedIds, cfg);
        return new MatchResult(gameId, matchedIds);
    }

    /** Runs inside tryFormMatch's transaction. Returns the new gameId. */
    private Long createRankedGame(List<Long> userIds, RankedConfig cfg) {
        List<MafiaUser> users = userRepository.findAllById(userIds);
        if (users.size() < userIds.size()) {
            throw new RuntimeException("Some matched users no longer exist");
        }
        MafiaUser host = users.get(0);

        Lobby lobby = new Lobby();
        lobby.setName("Ranked Match");
        lobby.setHost(host);
        lobby.setMaxPlayers(cfg.getPlayerCount());
        lobby.setPublicLobby(false);
        lobby.setLocked(true);
        lobby.setCreatedAt(LocalDateTime.now());
        lobby.setMafiaCount(cfg.getMafiaCount());
        lobby.setIncludeSheriff(cfg.isIncludeSheriff());
        lobby.setIncludeDoctor(cfg.isIncludeDoctor());
        lobby.setIncludeJester(cfg.isIncludeJester());
        lobby.setIncludeMutilator(cfg.isIncludeMutilator());
        lobby.setDoctorSelfSaveLimit(cfg.getDoctorSelfSaveLimit());
        lobby.setSheriffInvestigationDelay(cfg.getSheriffInvestigationDelay());
        lobby.setNightDurationSeconds(cfg.getNightDurationSeconds());
        lobby.setDayDurationSeconds(cfg.getDayDurationSeconds());
        lobby.setVotingDurationSeconds(cfg.getVotingDurationSeconds());
        lobbyRepository.save(lobby);

        for (MafiaUser u : users) {
            LobbyPlayer lp = new LobbyPlayer();
            lp.setLobby(lobby);
            lp.setUser(u);
            lp.setReady(true);
            lp.setJoinedAt(LocalDateTime.now());
            lobbyPlayerRepository.save(lp);
            lobby.getLobbyPlayers().add(lp);
        }

        var startEvent = gameService.startGameForLobby(lobby, true);
        return startEvent.gameId();
    }
}
