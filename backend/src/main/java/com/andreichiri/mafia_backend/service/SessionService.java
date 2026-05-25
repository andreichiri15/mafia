package com.andreichiri.mafia_backend.service;

import com.andreichiri.mafia_backend.dto.SessionDTO;
import com.andreichiri.mafia_backend.entity.Game;
import com.andreichiri.mafia_backend.entity.GamePlayer;
import com.andreichiri.mafia_backend.entity.Lobby;
import com.andreichiri.mafia_backend.entity.LobbyPlayer;
import com.andreichiri.mafia_backend.repositories.GamePlayerRepository;
import com.andreichiri.mafia_backend.repositories.GameRepository;
import com.andreichiri.mafia_backend.repositories.LobbyPlayerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class SessionService {

    @Autowired
    private LobbyPlayerRepository lobbyPlayerRepository;
    @Autowired
    private GameRepository gameRepository;
    @Autowired
    private GamePlayerRepository gamePlayerRepository;

    /**
     * Returns the user's current active lobby/game session, or null if they
     * are not currently in any lobby.
     */
    @Transactional(readOnly = true)
    public SessionDTO.SessionInfo getSession(Long userId) {
        Optional<LobbyPlayer> lp = lobbyPlayerRepository.findFirstByUserUserId(userId);
        if (lp.isEmpty()) return null;

        Lobby lobby = lp.get().getLobby();
        if (lobby == null) return null;

        Long gameId = null;
        String phase = null;
        boolean alive = true;

        Optional<Game> gameOpt = gameRepository.findByLobbyId(lobby.getId());
        if (gameOpt.isPresent() && gameOpt.get().getGamePhase() != Game.GamePhase.GAME_OVER) {
            Game game = gameOpt.get();
            gameId = game.getId();
            phase = game.getGamePhase().name();
            alive = gamePlayerRepository.findByGameIdAndUserId(game.getId(), userId)
                    .map(GamePlayer::getAlive)
                    .orElse(false);
        }

        return new SessionDTO.SessionInfo(
                lobby.getId(),
                lobby.getName(),
                gameId,
                phase,
                alive
        );
    }
}
