package com.andreichiri.mafia_backend.repositories;

import com.andreichiri.mafia_backend.entity.Lobby;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LobbyRepository extends JpaRepository<Lobby, Long> {
    public List<Lobby> getLobbiesByNameAndPublicLobby(String name, boolean publicLobby);

    public List<Lobby> getLobbiesByPublicLobby(boolean publicLobby);
}
