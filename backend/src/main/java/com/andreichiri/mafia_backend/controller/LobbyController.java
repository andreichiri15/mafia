package com.andreichiri.mafia_backend.controller;

import com.andreichiri.mafia_backend.dto.LobbyDTO;
import com.andreichiri.mafia_backend.dto.LobbySummaryReport;
import com.andreichiri.mafia_backend.security.UserPrincipal;
import com.andreichiri.mafia_backend.service.LobbyChatService;
import com.andreichiri.mafia_backend.service.LobbyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/lobbies")
public class LobbyController {
    @Autowired
    private LobbyService lobbyService;
    @Autowired
    private LobbyChatService lobbyChatService;

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

    @PostMapping("/{id}/bots")
    public ResponseEntity<?> addBot(@PathVariable Long id) {
        return lobbyService.addBot(id);
    }

    @DeleteMapping("/{id}/bots/{botUserId}")
    public ResponseEntity<?> removeBot(@PathVariable Long id, @PathVariable Long botUserId) {
        return lobbyService.removeBot(id, botUserId);
    }

    @PutMapping("/{id}/settings")
    public ResponseEntity<?> updateSettings(
            @PathVariable Long id,
            @RequestBody LobbyDTO.GameSettings settings
    ) {
        return lobbyService.updateSettings(id, settings);
    }

    @GetMapping("/invite/{token}")
    public ResponseEntity<?> resolveInvite(@PathVariable String token) {
        LobbyDTO.InviteResolution res = lobbyService.resolveInviteToken(token);
        return res != null ? ResponseEntity.ok(res) : ResponseEntity.status(404).body("Invite not found");
    }

    @PostMapping("/{id}/invite/{friendUserId}")
    public ResponseEntity<?> inviteFriend(@PathVariable Long id, @PathVariable Long friendUserId) {
        return lobbyService.inviteFriend(id, friendUserId);
    }

    @GetMapping("/{id}/messages")
    public ResponseEntity<?> getLobbyChatHistory(@PathVariable Long id) {
        try {
            UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext()
                    .getAuthentication().getPrincipal();
            List<Map<String, Object>> history = lobbyChatService.getHistory(id, principal.userId());
            return ResponseEntity.ok(history);
        } catch (RuntimeException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        }
    }
}
