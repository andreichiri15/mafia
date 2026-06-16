package com.andreichiri.mafia_backend.service;

import com.andreichiri.mafia_backend.entity.Game;
import com.andreichiri.mafia_backend.entity.GameAction;
import com.andreichiri.mafia_backend.entity.GamePlayer;
import com.andreichiri.mafia_backend.repositories.GameActionRepository;
import com.andreichiri.mafia_backend.repositories.GameRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Pure ELO math + role-winrate cache.
 *
 * Formula per player:
 *   teamDelta     = K * (actualScore - expectedScore)
 *   personalDelta = K * personalScore        (personalScore clamped to [-1, +1])
 *   finalDelta    = 0.8 * teamDelta + 0.2 * personalDelta   (jester: 100% teamDelta)
 *
 * K-factor (controls swing size) depends on role.
 * expectedScore comes from observed historical winrate for that role.
 */
@Service
public class EloService {

    @Autowired
    private GameRepository gameRepository;
    @Autowired
    private GameActionRepository gameActionRepository;

    public static final int DEFAULT_ELO = 1000;
    public static final int QUIT_PENALTY = 30;

    /** K-factor per role (controls how much ELO swings). */
    private int kFactor(GamePlayer.Role role) {
        return switch (role) {
            case VILLAGER -> 16;
            case MAFIA, MUTILATOR, JESTER -> 24;
            case SHERIFF, DOCTOR -> 32;
        };
    }

    /**
     * Fallback role winrates used until we have enough history to compute them.
     * Refreshed from the DB via {@link #refreshWinrates()}.
     */
    private final Map<GamePlayer.Role, Double> winrateCache = new ConcurrentHashMap<>(Map.of(
            GamePlayer.Role.MAFIA, 0.45,
            GamePlayer.Role.MUTILATOR, 0.45,
            GamePlayer.Role.VILLAGER, 0.50,
            GamePlayer.Role.SHERIFF, 0.50,
            GamePlayer.Role.DOCTOR, 0.50,
            GamePlayer.Role.JESTER, 0.10
    ));

    public double expectedScore(GamePlayer.Role role) {
        return winrateCache.getOrDefault(role, 0.5);
    }

    /**
     * Walks finished ranked games and recomputes winrate per role.
     * Lazy refresh — call this on a schedule or after enough new data lands.
     */
    public void refreshWinrates() {
        List<Game> games = gameRepository.findAll().stream()
                .filter(g -> Boolean.TRUE.equals(g.getRanked()) && g.getWinningTeam() != null)
                .toList();
        Map<GamePlayer.Role, int[]> counts = new java.util.EnumMap<>(GamePlayer.Role.class);
        for (GamePlayer.Role r : GamePlayer.Role.values()) counts.put(r, new int[]{0, 0});

        for (Game g : games) {
            for (GamePlayer gp : g.getGamePlayers()) {
                if (gp.getRole() == null) continue;
                int[] pw = counts.get(gp.getRole());
                pw[0]++;
                if (didWin(gp.getRole(), g.getWinningTeam())) pw[1]++;
            }
        }
        for (GamePlayer.Role role : GamePlayer.Role.values()) {
            int[] pw = counts.get(role);
            if (pw[0] >= 20) {
                winrateCache.put(role, (double) pw[1] / pw[0]);
            }
        }
    }

    public boolean didWin(GamePlayer.Role role, String winningTeam) {
        if (role == null || winningTeam == null) return false;
        return switch (role) {
            case MAFIA, MUTILATOR -> "MAFIA_WIN".equals(winningTeam);
            case VILLAGER, SHERIFF, DOCTOR -> "VILLAGER_WIN".equals(winningTeam);
            case JESTER -> "JESTER_WIN".equals(winningTeam);
        };
    }

    /**
     * Computes the final ELO delta for one player at the end of a ranked game.
     *
     * @param maxRound the round number the game ended on (used for progress curve)
     */
    public int computeDelta(Game game, GamePlayer player, int maxRound) {
        if (player.getRole() == null || game.getWinningTeam() == null) return 0;

        int k = kFactor(player.getRole());
        double actual = didWin(player.getRole(), game.getWinningTeam()) ? 1.0 : 0.0;
        double expected = expectedScore(player.getRole());
        double teamDelta = k * (actual - expected);

        // Jester: 100% team-based (user spec)
        if (player.getRole() == GamePlayer.Role.JESTER) {
            return (int) Math.round(teamDelta);
        }

        double personalScore = computePersonalScore(game, player, maxRound);
        double personalDelta = k * personalScore;

        return (int) Math.round(0.8 * teamDelta + 0.2 * personalDelta);
    }

    /**
     * Personal score in [-1, +1]. Computed from the GameAction history + the
     * player's final alive/death state. Each event's weight is scaled by the
     * round it occurred in (later rounds count more): weight = 0.5 + 0.5 * min(round, 5) / 5.
     */
    double computePersonalScore(Game game, GamePlayer player, int maxRound) {
        double score = 0.0;
        Long userId = player.getUser().getUserId();

        // ---- End-of-game outcomes (use maxRound for weighting) ----
        double finalWeight = roundWeight(maxRound);
        switch (player.getRole()) {
            case MAFIA, MUTILATOR -> {
                if (Boolean.TRUE.equals(player.getAlive())) {
                    score += 0.25 * finalWeight; // survived
                }
                if (player.getDeathCause() == GamePlayer.DeathCause.VOTED_OUT) {
                    score -= 0.30 * finalWeight; // got found out
                }
            }
            default -> { /* villager-side end-of-game contributions are action-based */ }
        }

        // ---- Per-action contributions ----
        List<GameAction> actions = gameActionRepository.findByGameId(game.getId());
        // Build a role lookup for resolving targets' roles
        Map<Long, GamePlayer.Role> roleByUserId = new java.util.HashMap<>();
        for (GamePlayer gp : game.getGamePlayers()) {
            if (gp.getUser() != null && gp.getRole() != null) {
                roleByUserId.put(gp.getUser().getUserId(), gp.getRole());
            }
        }

        for (GameAction a : actions) {
            double w = roundWeight(a.getRound());
            Long actorId = a.getActor() != null ? a.getActor().getUserId() : null;
            Long targetId = a.getTarget() != null ? a.getTarget().getUserId() : null;
            if (actorId == null) continue;

            GamePlayer.Role targetRole = roleByUserId.get(targetId);

            switch (player.getRole()) {
                case MAFIA -> {
                    if (a.getActionType() == GameAction.ActionType.MAFIA_KILL
                            && actorId.equals(userId)
                            && (targetRole == GamePlayer.Role.SHERIFF || targetRole == GamePlayer.Role.DOCTOR)) {
                        score += 0.20 * w;
                    }
                }
                case SHERIFF -> {
                    if (a.getActionType() == GameAction.ActionType.INVESTIGATE && actorId.equals(userId)) {
                        if (targetRole == GamePlayer.Role.MAFIA || targetRole == GamePlayer.Role.MUTILATOR) {
                            score += 0.30 * w;
                        } else {
                            score -= 0.05 * w;
                        }
                    }
                }
                case DOCTOR -> {
                    // Saved someone the mafia tried to kill that round
                    if (a.getActionType() == GameAction.ActionType.HEALED && actorId.equals(userId)) {
                        boolean mafiaTargetedSame = actions.stream().anyMatch(other ->
                                other.getActionType() == GameAction.ActionType.MAFIA_KILL
                                        && other.getRound() == a.getRound()
                                        && other.getTarget() != null && other.getTarget().getUserId().equals(targetId)
                        );
                        if (mafiaTargetedSame) score += 0.30 * w;
                    }
                }
                case VILLAGER -> {
                    if (a.getActionType() == GameAction.ActionType.VOTE && actorId.equals(userId)) {
                        if (targetRole == GamePlayer.Role.MAFIA || targetRole == GamePlayer.Role.MUTILATOR) {
                            score += 0.20 * w;
                        } else {
                            score -= 0.05 * w;
                        }
                    }
                }
                case MUTILATOR, JESTER -> { /* nothing specific from voting/investigation */ }
            }
        }

        // Clamp into [-1, +1]
        if (score > 1.0) score = 1.0;
        if (score < -1.0) score = -1.0;
        return score;
    }

    /** 0.5 at round 1, ramping up to 1.0 at round 5+. */
    private double roundWeight(int round) {
        int r = Math.min(Math.max(round, 1), 5);
        return 0.5 + 0.5 * (r / 5.0);
    }
}
