package com.andreichiri.mafia_backend.controller;

import com.andreichiri.mafia_backend.dto.LiveKitDTO;
import com.andreichiri.mafia_backend.entity.Game;
import com.andreichiri.mafia_backend.entity.GamePlayer;
import com.andreichiri.mafia_backend.repositories.GamePlayerRepository;
import com.andreichiri.mafia_backend.repositories.GameRepository;
import com.andreichiri.mafia_backend.repositories.LobbyPlayerRepository;
import com.andreichiri.mafia_backend.security.UserPrincipal;
import com.andreichiri.mafia_backend.service.LiveKitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/livekit")
public class LiveKitController {

    @Autowired
    private LiveKitService liveKitService;
    @Autowired
    private LobbyPlayerRepository lobbyPlayerRepository;
    @Autowired
    private GameRepository gameRepository;
    @Autowired
    private GamePlayerRepository gamePlayerRepository;

    @PostMapping("/token")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getToken(@RequestBody LiveKitDTO.TokenRequest request) {
        if (!liveKitService.isConfigured()) {
            return ResponseEntity.status(503).body("Voice chat is not configured on the server");
        }
        if (request == null || request.scope() == null || request.id() == null) {
            return ResponseEntity.badRequest().body("scope and id are required");
        }

        UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        Long userId = principal.userId();
        String displayName = principal.username();
        String identity = userId.toString();

        String roomName;
        boolean canPublish;

        switch (request.scope()) {
            case LOBBY -> {
                Long lobbyId = request.id();
                if (!lobbyPlayerRepository.existsByLobbyIdAndUserUserId(lobbyId, userId)) {
                    return ResponseEntity.status(403).body("Not a member of this lobby");
                }
                roomName = "lobby-" + lobbyId;
                canPublish = true;
            }
            case GAME_MAIN -> {
                Long gameId = request.id();
                GamePlayer gp = gamePlayerRepository.findByGameIdAndUserId(gameId, userId)
                        .orElse(null);
                if (gp == null) {
                    return ResponseEntity.status(403).body("Not a participant in this game");
                }
                Game game = gameRepository.findById(gameId).orElse(null);
                roomName = "game-" + gameId;

                // Listening-only if dead, or if it's NIGHT (non-mafia don't talk at night;
                // mafia switch to the mafia room during NIGHT, so they're not in this room then)
                boolean isNight = game != null && game.getGamePhase() == Game.GamePhase.NIGHT;
                canPublish = Boolean.TRUE.equals(gp.getAlive()) && !isNight;
            }
            case GAME_MAFIA -> {
                Long gameId = request.id();
                GamePlayer gp = gamePlayerRepository.findByGameIdAndUserId(gameId, userId)
                        .orElse(null);
                if (gp == null) {
                    return ResponseEntity.status(403).body("Not a participant in this game");
                }
                // Only MAFIA role gets into the mafia room (mutilator does not, per game design)
                if (gp.getRole() != GamePlayer.Role.MAFIA) {
                    return ResponseEntity.status(403).body("Mafia voice is restricted to mafia members");
                }
                // The mafia room is only meaningful while the game is still running
                Game game = gameRepository.findById(gameId).orElse(null);
                if (game == null || game.getGamePhase() == Game.GamePhase.GAME_OVER) {
                    return ResponseEntity.status(400).body("Game is not active");
                }
                roomName = "game-" + gameId + "-mafia";
                canPublish = Boolean.TRUE.equals(gp.getAlive());
            }
            default -> {
                return ResponseEntity.badRequest().body("Unknown scope");
            }
        }

        String jwt = liveKitService.generateToken(identity, displayName, roomName, canPublish, true);
        return ResponseEntity.ok(new LiveKitDTO.TokenResponse(jwt, liveKitService.getUrl(), roomName, canPublish));
    }
}
