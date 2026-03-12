package com.andreichiri.mafia_backend.service;


import com.andreichiri.mafia_backend.dto.LobbyDTO;
import com.andreichiri.mafia_backend.dto.LobbySummaryReport;
import com.andreichiri.mafia_backend.entity.Lobby;
import com.andreichiri.mafia_backend.repositories.LobbyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LobbyService {
    @Autowired
    public LobbyRepository lobbyRepository;

    public List<LobbySummaryReport> searchPublicLobbies(String searchName) {
        if (searchName != null) {
            return lobbyRepository.getLobbiesByNameAndPublicLobby(searchName, true)
                    .stream()
                    .map(this::toLobbySummary)
                    .collect(Collectors.toList());
        }

        return lobbyRepository.getLobbiesByPublicLobby(true)
                .stream()
                .map(this::toLobbySummary)
                .collect(Collectors.toList());
    }

    public LobbyDTO.LobbyDetailResponse createLobby(LobbyDTO.CreateLobbyRequest createLobbyRequest) {
        Lobby newLobby = new Lobby();

        newLobby.setName(createLobbyRequest.name());
        newLobby.setCreatedAt(LocalDateTime.now());
        newLobby.setLobbyPlayers(new ArrayList<>());
        newLobby.setMaxPlayers(createLobbyRequest.maxPlayers());
        newLobby.setLocked(createLobbyRequest.isLocked());
        newLobby.setPassword(createLobbyRequest.password());

        lobbyRepository.save(newLobby);

        return new LobbyDTO.LobbyDetailResponse(
                newLobby.getId(),
                newLobby.getName(),
                "owner",
                newLobby.getMaxPlayers(),
                1,
                newLobby.getPassword() != null,
                newLobby.isLocked(),
                newLobby.getCreatedAt()
        );
    }

    public void updateLobby() {

    }

    public LobbySummaryReport toLobbySummary(Lobby lobby) {
        return new LobbySummaryReport(
            lobby.getId(),
            lobby.getName(),
            lobby.getHost().getUsername(),
            lobby.getMaxPlayers(),
            lobby.getLobbyPlayers().size(),
            lobby.getCreatedAt()
        );
    }
}
