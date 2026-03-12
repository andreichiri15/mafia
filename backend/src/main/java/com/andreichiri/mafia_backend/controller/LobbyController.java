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
    public LobbyService lobbyService;

    @GetMapping
    public ResponseEntity<List<LobbySummaryReport>> searchPublicLobbies(
            @RequestParam(required = false) String searchName
    ) {

        return ResponseEntity.ok(lobbyService.searchPublicLobbies(searchName));
    }

    @PostMapping
    public ResponseEntity<?> createLobby(
            @RequestParam LobbyDTO.CreateLobbyRequest createLobbyRequest
    ) {
        LobbyDTO.LobbyDetailResponse lobbyDetailResponse = lobbyService.createLobby(createLobbyRequest);

        return lobbyDetailResponse != null ? ResponseEntity.ok(lobbyDetailResponse) : ResponseEntity.badRequest().body("Couldn't crate lobby");
    }
}