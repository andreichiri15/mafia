package com.andreichiri.mafia_backend.service;

import com.andreichiri.mafia_backend.dto.ProfileDTO;
import com.andreichiri.mafia_backend.entity.Game;
import com.andreichiri.mafia_backend.entity.GamePlayer;
import com.andreichiri.mafia_backend.entity.MafiaUser;
import com.andreichiri.mafia_backend.repositories.GamePlayerRepository;
import com.andreichiri.mafia_backend.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ProfileService {

    private static final int MATCH_HISTORY_LIMIT = 20;

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private GamePlayerRepository gamePlayerRepository;

    @Transactional(readOnly = true)
    public ProfileDTO.ProfileResponse getProfile(Long userId) {
        MafiaUser user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<GamePlayer> finished = gamePlayerRepository.findFinishedGamesByUserId(userId);

        int totalGames = finished.size();
        int totalWins = (int) finished.stream()
                .filter(gp -> playerWon(gp.getRole(), gp.getGame().getWinningTeam()))
                .count();
        double winRate = totalGames == 0 ? 0.0 : (double) totalWins / totalGames;

        // Survival rate: % of games where player was alive at game end
        double survivalRate = totalGames == 0 ? 0.0
                : (double) finished.stream().filter(gp -> Boolean.TRUE.equals(gp.getAlive())).count() / totalGames;

        // Average duration
        long totalSeconds = finished.stream()
                .filter(gp -> gp.getGame().getEndedAt() != null && gp.getGame().getStartedAt() != null)
                .mapToLong(gp -> Duration.between(gp.getGame().getStartedAt(), gp.getGame().getEndedAt()).getSeconds())
                .sum();
        long avgSeconds = totalGames == 0 ? 0 : totalSeconds / totalGames;

        // Per-role stats
        Map<GamePlayer.Role, int[]> tally = new EnumMap<>(GamePlayer.Role.class);
        for (GamePlayer gp : finished) {
            int[] pw = tally.computeIfAbsent(gp.getRole(), r -> new int[]{0, 0});
            pw[0]++;
            if (playerWon(gp.getRole(), gp.getGame().getWinningTeam())) pw[1]++;
        }
        Map<String, ProfileDTO.RoleStats> roleStats = new java.util.LinkedHashMap<>();
        for (GamePlayer.Role role : GamePlayer.Role.values()) {
            int[] pw = tally.getOrDefault(role, new int[]{0, 0});
            double rwr = pw[0] == 0 ? 0.0 : (double) pw[1] / pw[0];
            roleStats.put(role.name(), new ProfileDTO.RoleStats(pw[0], pw[1], rwr));
        }

        // Favorite role: most-played; null if user hasn't played
        String favoriteRole = tally.entrySet().stream()
                .max((a, b) -> Integer.compare(a.getValue()[0], b.getValue()[0]))
                .map(e -> e.getKey().name())
                .orElse(null);

        // Match history (most recent first, limited)
        List<ProfileDTO.MatchHistoryEntry> history = finished.stream()
                .limit(MATCH_HISTORY_LIMIT)
                .map(gp -> {
                    Game g = gp.getGame();
                    long dur = (g.getStartedAt() != null && g.getEndedAt() != null)
                            ? Duration.between(g.getStartedAt(), g.getEndedAt()).getSeconds()
                            : 0L;
                    return new ProfileDTO.MatchHistoryEntry(
                            g.getId(),
                            g.getLobby() != null ? g.getLobby().getName() : "(deleted lobby)",
                            gp.getRole() != null ? gp.getRole().name() : null,
                            g.getWinningTeam(),
                            playerWon(gp.getRole(), g.getWinningTeam()),
                            Boolean.TRUE.equals(gp.getAlive()),
                            gp.getDeathCause() != null ? gp.getDeathCause().name() : null,
                            g.getStartedAt(),
                            g.getEndedAt(),
                            dur
                    );
                })
                .collect(Collectors.toList());

        return new ProfileDTO.ProfileResponse(
                user.getUserId(),
                user.getUsername(),
                user.getDateJoined(),
                totalGames,
                totalWins,
                winRate,
                favoriteRole,
                survivalRate,
                avgSeconds,
                roleStats,
                history
        );
    }

    private boolean playerWon(GamePlayer.Role role, String winningTeam) {
        if (role == null || winningTeam == null) return false;
        return switch (role) {
            case MAFIA, MUTILATOR -> "MAFIA_WIN".equals(winningTeam);
            case VILLAGER, SHERIFF, DOCTOR -> "VILLAGER_WIN".equals(winningTeam);
            case JESTER -> "JESTER_WIN".equals(winningTeam);
        };
    }
}
