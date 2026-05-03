package com.andreichiri.mafia_backend.repositories;

import com.andreichiri.mafia_backend.entity.LobbyPlayer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LobbyPlayerRepository extends JpaRepository<LobbyPlayer, Long> {
    Optional<LobbyPlayer> findByLobbyIdAndUserUserId(Long lobbyId, Long userId);
    boolean existsByLobbyIdAndUserUserId(Long lobbyId, Long userId);
    void deleteByLobbyIdAndUserUserId(Long lobbyId, Long userId);
    boolean existsByUserUserId(Long userId);
}
