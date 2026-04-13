package com.andreichiri.mafia_backend.controller;

import com.andreichiri.mafia_backend.dto.LobbyDTO;
import com.andreichiri.mafia_backend.dto.LobbySummaryReport;
import com.andreichiri.mafia_backend.service.LobbyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/lobbies")
public class LobbyController {
    @Autowired
    private LobbyService lobbyService;

    @GetMapping
    public ResponseEntity<List<LobbySummaryReport>> searchPublicLobbies(
            @RequestParam(required = false) String searchName
    ) {
        return ResponseEntity.ok(lobbyService.searchPublicLobbies(searchName));
    }

    @PostMapping
    public ResponseEntity<?> createLobby(
            @RequestBody LobbyDTO.CreateLobbyRequest createLobbyRequest
    ) {
        LobbyDTO.LobbyDetailResponse response = lobbyService.createLobby(createLobbyRequest);
        return response != null
                ? ResponseEntity.ok(response)
                : ResponseEntity.badRequest().body("Couldn't create lobby");
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getLobbyDetail(@PathVariable Long id) {
        LobbyDTO.LobbyDetailResponse response = lobbyService.getLobbyDetail(id);
        return response != null
                ? ResponseEntity.ok(response)
                : ResponseEntity.status(404).body("Lobby not found");
    }

    @PostMapping("/{id}/join")
    public ResponseEntity<?> joinLobby(
            @PathVariable Long id,
            @RequestBody(required = false) LobbyDTO.JoinLobbyRequest request
    ) {
        String password = request != null ? request.password() : null;
        return lobbyService.joinLobby(id, password);
    }

    @PostMapping("/{id}/leave")
    public ResponseEntity<?> leaveLobby(@PathVariable Long id) {
        return lobbyService.leaveLobby(id);
    }

    @PutMapping("/{id}/ready")
    public ResponseEntity<?> toggleReady(@PathVariable Long id) {
        return lobbyService.toggleReady(id);
    }
}
