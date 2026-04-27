package com.andreichiri.mafia_backend.controller;

import com.andreichiri.mafia_backend.dto.GameDTO;
import com.andreichiri.mafia_backend.security.UserPrincipal;
import com.andreichiri.mafia_backend.service.GameService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class GameController {

    @Autowired
    private GameService gameService;

    @PostMapping("/lobbies/{lobbyId}/start")
    public ResponseEntity<?> startGame(@PathVariable Long lobbyId) {
        try {
            GameDTO.GameStartEvent event = gameService.startGame(lobbyId);
            return ResponseEntity.ok(event);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/games/{gameId}")
    public ResponseEntity<?> getGameState(@PathVariable Long gameId) {
        try {
            UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext()
                    .getAuthentication().getPrincipal();
            GameDTO.GameStateResponse state = gameService.getGameState(gameId, principal.userId());
            return ResponseEntity.ok(state);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
