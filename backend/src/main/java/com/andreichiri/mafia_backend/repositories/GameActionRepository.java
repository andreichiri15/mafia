package com.andreichiri.mafia_backend.repositories;

import com.andreichiri.mafia_backend.entity.GameAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GameActionRepository extends JpaRepository<GameAction, Long> {
    List<GameAction> findByGameIdAndRound(Long gameId, Integer round);
    List<GameAction> findByGameId(Long gameId);
}
