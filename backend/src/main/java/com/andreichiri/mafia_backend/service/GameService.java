package com.andreichiri.mafia_backend.service;

import com.andreichiri.mafia_backend.dto.GameDTO;
import com.andreichiri.mafia_backend.entity.*;
import com.andreichiri.mafia_backend.repositories.*;
import com.andreichiri.mafia_backend.security.UserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GameService {

    @Autowired
    private GameRepository gameRepository;
    @Autowired
    private GamePlayerRepository gamePlayerRepository;
    @Autowired
    private GameActionRepository gameActionRepository;
    @Autowired
    private LobbyRepository lobbyRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleAssignmentService roleAssignmentService;
    @Autowired
    private PhaseTimerService phaseTimerService;
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    @Autowired
    @Lazy
    private GameService self;

    private static final int MIN_PLAYERS = 4;

    @Transactional
    public GameDTO.GameStartEvent startGame(Long lobbyId) {
        UserPrincipal principal = getAuthenticatedUser();
        Lobby lobby = lobbyRepository.findById(lobbyId)
                .orElseThrow(() -> new RuntimeException("Lobby not found"));

        if (!lobby.getHost().getUserId().equals(principal.userId())) {
            throw new RuntimeException("Only the host can start the game");
        }

        if (lobby.getLobbyPlayers().size() < MIN_PLAYERS) {
            throw new RuntimeException("Need at least " + MIN_PLAYERS + " players to start");
        }

        if (gameRepository.findByLobbyId(lobbyId).isPresent()) {
            throw new RuntimeException("Game already started for this lobby");
        }

        // Create game
        Game game = new Game();
        game.setLobby(lobby);
        game.setCurrentRound(1);
        game.setGamePhase(Game.GamePhase.NIGHT);
        game.setStartedAt(LocalDateTime.now());
        gameRepository.save(game);

        // Lock the lobby
        lobby.setLocked(true);
        lobbyRepository.save(lobby);

        // Assign roles and create game players
        List<LobbyPlayer> lobbyPlayers = lobby.getLobbyPlayers();
        List<GamePlayer.Role> roles = roleAssignmentService.assignRoles(lobbyPlayers.size());

        for (int i = 0; i < lobbyPlayers.size(); i++) {
            GamePlayer gp = new GamePlayer();
            gp.setGame(game);
            gp.setUser(lobbyPlayers.get(i).getUser());
            gp.setRole(roles.get(i));
            gp.setAlive(true);
            gamePlayerRepository.save(gp);
            game.getGamePlayers().add(gp);
        }

        // Schedule night phase timer FIRST so all broadcast state has a valid phaseEndTime
        schedulePhaseTimer(game.getId());

        // Notify all players about game start
        GameDTO.GameStartEvent startEvent = new GameDTO.GameStartEvent(game.getId(), lobbyId);
        messagingTemplate.convertAndSend("/topic/lobby/" + lobbyId + "/game-start", startEvent);

        // Send each player their initial state (includes role + phaseEndTime)
        broadcastPhaseChange(game);

        return startEvent;
    }

    public GameDTO.GameStateResponse getGameState(Long gameId, Long userId) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new RuntimeException("Game not found"));
        return buildGameState(game, userId);
    }

    @Transactional
    public void submitAction(Long gameId, Long userId, GameAction.ActionType actionType, Long targetUserId) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new RuntimeException("Game not found"));

        if (game.getGamePhase() == Game.GamePhase.GAME_OVER) {
            throw new RuntimeException("Game is over");
        }

        GamePlayer actor = gamePlayerRepository.findByGameIdAndUserId(gameId, userId)
                .orElseThrow(() -> new RuntimeException("Player not in this game"));

        if (!actor.getAlive()) {
            throw new RuntimeException("Dead players cannot perform actions");
        }

        MafiaUser target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new RuntimeException("Target not found"));

        validateAction(game, actor, actionType, targetUserId);

        // Save the action
        GameAction action = new GameAction();
        action.setGame(game);
        action.setRound(game.getCurrentRound());
        action.setGamePhase(game.getGamePhase());
        action.setActor(actor.getUser());
        action.setTarget(target);
        action.setActionType(actionType);
        action.setExecutedAt(LocalDateTime.now());
        gameActionRepository.save(action);

        // Check if all expected actions for this phase are in
        if (allActionsSubmitted(game)) {
            phaseTimerService.cancelTimer(gameId);
            resolvePhase(gameId);
        }
    }

    @Transactional
    public void resolvePhase(Long gameId) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new RuntimeException("Game not found"));

        List<String> events = new ArrayList<>();

        switch (game.getGamePhase()) {
            case NIGHT -> {
                events = resolveNight(game);
                // Check win condition after night
                String winner = checkWinCondition(game);
                if (winner != null) {
                    endGame(game, winner);
                    return;
                }
                game.setGamePhase(Game.GamePhase.DAY);
            }
            case DAY -> {
                game.setGamePhase(Game.GamePhase.VOTING);
            }
            case VOTING -> {
                events = resolveVoting(game);
                // Check win condition after voting
                String winner = checkWinCondition(game);
                if (winner != null) {
                    endGame(game, winner);
                    return;
                }
                game.setCurrentRound(game.getCurrentRound() + 1);
                game.setGamePhase(Game.GamePhase.NIGHT);
            }
            case GAME_OVER -> {
                return;
            }
        }

        gameRepository.save(game);

        // Schedule next phase timer FIRST so the broadcast state has a valid phaseEndTime
        schedulePhaseTimer(gameId);

        // Broadcast phase result
        GameDTO.PhaseResultEvent phaseResult = new GameDTO.PhaseResultEvent(
                game.getGamePhase(),
                game.getCurrentRound(),
                events.stream().filter(e -> e.startsWith("ELIMINATED:")).findFirst()
                        .map(e -> e.substring("ELIMINATED:".length())).orElse(null),
                events
        );
        messagingTemplate.convertAndSend("/topic/game/" + gameId + "/phase", phaseResult);

        // Send updated state to each player (now includes the new phaseEndTime)
        broadcastPhaseChange(game);
    }

    private List<String> resolveNight(Game game) {
        List<String> events = new ArrayList<>();
        int round = game.getCurrentRound();
        Long gameId = game.getId();

        // Resolve mafia kill
        List<GameAction> killActions = gameActionRepository
                .findByGameIdAndRoundAndActionType(gameId, round, GameAction.ActionType.MAFIA_KILL);

        Long killTargetId = getMajorityTarget(killActions);

        // Resolve sheriff investigation
        List<GameAction> investigateActions = gameActionRepository
                .findByGameIdAndRoundAndActionType(gameId, round, GameAction.ActionType.INVESTIGATE);

        if (!investigateActions.isEmpty()) {
            GameAction investigate = investigateActions.get(0);
            GamePlayer investigatedPlayer = gamePlayerRepository
                    .findByGameIdAndUserId(gameId, investigate.getTarget().getUserId())
                    .orElse(null);
            if (investigatedPlayer != null) {
                boolean isMafia = investigatedPlayer.getRole() == GamePlayer.Role.MAFIA;
                investigate.setResult(isMafia ? "MAFIA" : "NOT_MAFIA");
                gameActionRepository.save(investigate);

                // Send investigation result privately to the sheriff (separate topic from state)
                messagingTemplate.convertAndSend(
                        "/topic/game/" + gameId + "/private/" + investigate.getActor().getUserId(),
                        Map.of("type", "INVESTIGATION_RESULT",
                                "target", investigatedPlayer.getUser().getUsername(),
                                "result", isMafia ? "MAFIA" : "NOT_MAFIA")
                );
            }
        }

        // Apply mafia kill (check if healed)
        if (killTargetId != null) {
            // Check if target was healed
            List<GameAction> healActions = gameActionRepository
                    .findByGameIdAndRoundAndActionType(gameId, round, GameAction.ActionType.HEALED);
            boolean healed = healActions.stream()
                    .anyMatch(h -> h.getTarget().getUserId().equals(killTargetId));

            if (!healed) {
                GamePlayer victim = gamePlayerRepository.findByGameIdAndUserId(gameId, killTargetId)
                        .orElse(null);
                if (victim != null && victim.getAlive()) {
                    victim.setAlive(false);
                    victim.setKilledAtRound(round);
                    victim.setDeathCause(GamePlayer.DeathCause.MAFIA_KILL);
                    gamePlayerRepository.save(victim);
                    events.add(victim.getUser().getUsername() + " was killed by the mafia during the night.");
                    events.add("ELIMINATED:" + victim.getUser().getUsername());
                }
            } else {
                events.add("The mafia attempted a kill, but the target was saved!");
            }
        } else {
            events.add("The mafia could not agree on a target.");
        }

        if (events.isEmpty() || events.stream().noneMatch(e -> e.contains("killed"))) {
            events.add(0, "Dawn breaks. Everyone survived the night.");
        }

        return events;
    }

    private List<String> resolveVoting(Game game) {
        List<String> events = new ArrayList<>();
        int round = game.getCurrentRound();
        Long gameId = game.getId();

        List<GameAction> voteActions = gameActionRepository
                .findByGameIdAndRoundAndActionType(gameId, round, GameAction.ActionType.VOTE);

        Long voteTargetId = getMajorityTarget(voteActions);

        if (voteTargetId != null) {
            // Check for tie
            Map<Long, Long> voteCounts = voteActions.stream()
                    .collect(Collectors.groupingBy(a -> a.getTarget().getUserId(), Collectors.counting()));
            long maxVotes = voteCounts.values().stream().mapToLong(Long::longValue).max().orElse(0);
            long tieCount = voteCounts.values().stream().filter(v -> v == maxVotes).count();

            if (tieCount > 1) {
                events.add("The vote was tied. No one was eliminated.");
            } else {
                GamePlayer victim = gamePlayerRepository.findByGameIdAndUserId(gameId, voteTargetId)
                        .orElse(null);
                if (victim != null && victim.getAlive()) {
                    victim.setAlive(false);
                    victim.setKilledAtRound(round);
                    victim.setDeathCause(GamePlayer.DeathCause.VOTED_OUT);
                    gamePlayerRepository.save(victim);
                    events.add(victim.getUser().getUsername() + " was voted out. They were a " + victim.getRole().name() + ".");
                    events.add("ELIMINATED:" + victim.getUser().getUsername());
                }
            }
        } else {
            events.add("No votes were cast. No one was eliminated.");
        }

        return events;
    }

    private String checkWinCondition(Game game) {
        List<GamePlayer> alivePlayers = game.getGamePlayers().stream()
                .filter(GamePlayer::getAlive)
                .toList();

        long mafiaAlive = alivePlayers.stream()
                .filter(p -> p.getRole() == GamePlayer.Role.MAFIA)
                .count();
        long nonMafiaAlive = alivePlayers.stream()
                .filter(p -> p.getRole() != GamePlayer.Role.MAFIA)
                .count();

        if (mafiaAlive == 0) return "VILLAGER_WIN";
        if (mafiaAlive >= nonMafiaAlive) return "MAFIA_WIN";
        return null;
    }

    private void endGame(Game game, String winner) {
        game.setGamePhase(Game.GamePhase.GAME_OVER);
        game.setEndedAt(LocalDateTime.now());
        gameRepository.save(game);

        phaseTimerService.cancelTimer(game.getId());

        // Build game over event with all roles revealed
        List<GameDTO.GamePlayerInfo> allPlayers = game.getGamePlayers().stream()
                .map(gp -> new GameDTO.GamePlayerInfo(
                        gp.getUser().getUserId(),
                        gp.getUser().getUsername(),
                        gp.getAlive(),
                        gp.getRole()
                ))
                .toList();

        GameDTO.GameOverEvent gameOver = new GameDTO.GameOverEvent(winner, allPlayers);
        messagingTemplate.convertAndSend("/topic/game/" + game.getId() + "/game-over", gameOver);
    }

    private void validateAction(Game game, GamePlayer actor, GameAction.ActionType actionType, Long targetUserId) {
        Game.GamePhase phase = game.getGamePhase();

        switch (actionType) {
            case MAFIA_KILL -> {
                if (phase != Game.GamePhase.NIGHT) throw new RuntimeException("Mafia can only kill at night");
                if (actor.getRole() != GamePlayer.Role.MAFIA) throw new RuntimeException("Only mafia can kill");
                // Can't kill self
                if (actor.getUser().getUserId().equals(targetUserId)) throw new RuntimeException("Cannot target yourself");
            }
            case INVESTIGATE -> {
                if (phase != Game.GamePhase.NIGHT) throw new RuntimeException("Sheriff can only investigate at night");
                if (actor.getRole() != GamePlayer.Role.SHERIFF) throw new RuntimeException("Only the sheriff can investigate");
                if (actor.getUser().getUserId().equals(targetUserId)) throw new RuntimeException("Cannot investigate yourself");
            }
            case VOTE -> {
                if (phase != Game.GamePhase.VOTING) throw new RuntimeException("Can only vote during voting phase");
            }
            case HEALED -> {
                if (phase != Game.GamePhase.NIGHT) throw new RuntimeException("Can only heal at night");
            }
        }

        // Check target is alive
        GamePlayer target = gamePlayerRepository.findByGameIdAndUserId(game.getId(), targetUserId)
                .orElseThrow(() -> new RuntimeException("Target not in this game"));
        if (!target.getAlive()) throw new RuntimeException("Cannot target dead players");

        // Check player hasn't already acted this round in this phase
        List<GameAction> existingActions = gameActionRepository
                .findByGameIdAndRoundAndGamePhase(game.getId(), game.getCurrentRound(), phase);
        boolean alreadyActed = existingActions.stream()
                .anyMatch(a -> a.getActor().getUserId().equals(actor.getUser().getUserId())
                        && a.getActionType() == actionType);
        if (alreadyActed) throw new RuntimeException("Already performed this action this round");
    }

    private boolean allActionsSubmitted(Game game) {
        int round = game.getCurrentRound();
        Long gameId = game.getId();
        Game.GamePhase phase = game.getGamePhase();

        List<GamePlayer> alivePlayers = game.getGamePlayers().stream()
                .filter(GamePlayer::getAlive)
                .toList();

        List<GameAction> currentActions = gameActionRepository
                .findByGameIdAndRoundAndGamePhase(gameId, round, phase);

        return switch (phase) {
            case NIGHT -> {
                long mafiaAlive = alivePlayers.stream()
                        .filter(p -> p.getRole() == GamePlayer.Role.MAFIA).count();
                long sheriffAlive = alivePlayers.stream()
                        .filter(p -> p.getRole() == GamePlayer.Role.SHERIFF).count();
                long mafiaActions = currentActions.stream()
                        .filter(a -> a.getActionType() == GameAction.ActionType.MAFIA_KILL).count();
                long sheriffActions = currentActions.stream()
                        .filter(a -> a.getActionType() == GameAction.ActionType.INVESTIGATE).count();
                yield mafiaActions >= mafiaAlive && sheriffActions >= sheriffAlive;
            }
            case VOTING -> {
                long voteCount = currentActions.stream()
                        .filter(a -> a.getActionType() == GameAction.ActionType.VOTE).count();
                yield voteCount >= alivePlayers.size();
            }
            default -> false; // DAY phase has no required actions — timer-only
        };
    }

    private Long getMajorityTarget(List<GameAction> actions) {
        if (actions.isEmpty()) return null;

        Map<Long, Long> counts = actions.stream()
                .collect(Collectors.groupingBy(a -> a.getTarget().getUserId(), Collectors.counting()));

        return counts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    private GameDTO.GameStateResponse buildGameState(Game game, Long userId) {
        GamePlayer myPlayer = game.getGamePlayers().stream()
                .filter(gp -> gp.getUser().getUserId().equals(userId))
                .findFirst()
                .orElse(null);

        boolean isGameOver = game.getGamePhase() == Game.GamePhase.GAME_OVER;
        boolean isMafia = myPlayer != null && myPlayer.getRole() == GamePlayer.Role.MAFIA;

        List<GameDTO.GamePlayerInfo> players = game.getGamePlayers().stream()
                .map(gp -> {
                    GamePlayer.Role visibleRole = null;
                    if (isGameOver) {
                        visibleRole = gp.getRole(); // reveal all roles at game over
                    } else if (isMafia && gp.getRole() == GamePlayer.Role.MAFIA) {
                        visibleRole = GamePlayer.Role.MAFIA; // mafia see each other
                    }
                    return new GameDTO.GamePlayerInfo(
                            gp.getUser().getUserId(),
                            gp.getUser().getUsername(),
                            gp.getAlive(),
                            visibleRole
                    );
                })
                .toList();

        return new GameDTO.GameStateResponse(
                game.getId(),
                game.getGamePhase(),
                game.getCurrentRound(),
                phaseTimerService.getPhaseEndTime(game.getId()),
                myPlayer != null ? myPlayer.getRole() : null,
                myPlayer != null && myPlayer.getAlive(),
                players,
                List.of()
        );
    }

    private void broadcastPhaseChange(Game game) {
        for (GamePlayer gp : game.getGamePlayers()) {
            GameDTO.GameStateResponse state = buildGameState(game, gp.getUser().getUserId());
            messagingTemplate.convertAndSend(
                    "/topic/game/" + game.getId() + "/state/" + gp.getUser().getUserId(),
                    state
            );
        }
    }

    private void schedulePhaseTimer(Long gameId) {
        Game game = gameRepository.findById(gameId).orElse(null);
        if (game == null || game.getGamePhase() == Game.GamePhase.GAME_OVER) return;

        // Use self-injected proxy so that the scheduled @Transactional call works
        phaseTimerService.schedulePhaseEnd(gameId, game.getGamePhase(), () -> self.resolvePhase(gameId));
    }

    private UserPrincipal getAuthenticatedUser() {
        return (UserPrincipal) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
    }
}
