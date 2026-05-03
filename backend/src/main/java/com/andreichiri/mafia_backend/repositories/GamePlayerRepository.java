package com.andreichiri.mafia_backend.repositories;

import com.andreichiri.mafia_backend.entity.GamePlayer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GamePlayerRepository extends JpaRepository<GamePlayer, Long> {
    List<GamePlayer> findByGameId(Long gameId);
    @Query("SELECT gp FROM GamePlayer gp WHERE gp.game.id = :gameId AND gp.user.userId = :userId")
    Optional<GamePlayer> findByGameIdAndUserId(Long gameId, Long userId);

    @Query("SELECT (count(gp) > 0) FROM GamePlayer gp WHERE gp.user.userId = :userId AND gp.game.gamePhase <> com.andreichiri.mafia_backend.entity.Game.GamePhase.GAME_OVER")
    boolean existsActiveByUserId(Long userId);
}
