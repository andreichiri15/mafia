package com.andreichiri.mafia_backend.repositories;

import com.andreichiri.mafia_backend.entity.GameAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.andreichiri.mafia_backend.entity.Game;

import java.util.List;

@Repository
public interface GameActionRepository extends JpaRepository<GameAction, Long> {
    List<GameAction> findByGameIdAndRound(Long gameId, Integer round);
    List<GameAction> findByGameId(Long gameId);
    List<GameAction> findByGameIdAndRoundAndGamePhase(Long gameId, Integer round, Game.GamePhase phase);
    List<GameAction> findByGameIdAndRoundAndActionType(Long gameId, Integer round, GameAction.ActionType actionType);
}
