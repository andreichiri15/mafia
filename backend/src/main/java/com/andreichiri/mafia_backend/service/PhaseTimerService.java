package com.andreichiri.mafia_backend.service;

import com.andreichiri.mafia_backend.entity.Game;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Service
public class PhaseTimerService {

    private static final int NIGHT_DURATION_SECONDS = 30;
    private static final int DAY_DURATION_SECONDS = 90;
    private static final int VOTING_DURATION_SECONDS = 30;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);
    private final ConcurrentHashMap<Long, ScheduledFuture<?>> activeTimers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Long> phaseEndTimes = new ConcurrentHashMap<>();

    public int getPhaseDuration(Game.GamePhase phase) {
        return switch (phase) {
            case NIGHT -> NIGHT_DURATION_SECONDS;
            case DAY -> DAY_DURATION_SECONDS;
            case VOTING -> VOTING_DURATION_SECONDS;
            case GAME_OVER -> 0;
        };
    }

    public void schedulePhaseEnd(Long gameId, Game.GamePhase phase, Runnable onPhaseEnd) {
        cancelTimer(gameId);

        int durationSeconds = getPhaseDuration(phase);
        if (durationSeconds <= 0) return;

        long endTime = System.currentTimeMillis() + (durationSeconds * 1000L);
        phaseEndTimes.put(gameId, endTime);

        ScheduledFuture<?> future = scheduler.schedule(() -> {
            activeTimers.remove(gameId);
            phaseEndTimes.remove(gameId);
            onPhaseEnd.run();
        }, durationSeconds, TimeUnit.SECONDS);

        activeTimers.put(gameId, future);
    }

    public void cancelTimer(Long gameId) {
        ScheduledFuture<?> existing = activeTimers.remove(gameId);
        if (existing != null) {
            existing.cancel(false);
        }
        phaseEndTimes.remove(gameId);
    }

    public long getPhaseEndTime(Long gameId) {
        return phaseEndTimes.getOrDefault(gameId, 0L);
    }
}
