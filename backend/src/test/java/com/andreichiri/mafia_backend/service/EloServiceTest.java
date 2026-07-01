package com.andreichiri.mafia_backend.service;

import com.andreichiri.mafia_backend.entity.Game;
import com.andreichiri.mafia_backend.entity.GameAction;
import com.andreichiri.mafia_backend.entity.GamePlayer;
import com.andreichiri.mafia_backend.entity.GamePlayer.Role;
import com.andreichiri.mafia_backend.entity.MafiaUser;
import com.andreichiri.mafia_backend.repositories.GameActionRepository;
import com.andreichiri.mafia_backend.repositories.GameRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;

/**
 * Teste pentru logica de calcul a ratingului (componenta de echipă,
 * componenta individuală, factorul K, clamp-ul și excepția Jester).
 */
@ExtendWith(MockitoExtension.class)
class EloServiceTest {

    @Mock
    private GameRepository gameRepository;
    @Mock
    private GameActionRepository gameActionRepository;

    @InjectMocks
    private EloService eloService;

    private MafiaUser user(long id) {
        MafiaUser u = new MafiaUser();
        u.setUserId(id);
        return u;
    }

    private GamePlayer player(MafiaUser u, Role role, boolean alive) {
        GamePlayer gp = new GamePlayer();
        gp.setUser(u);
        gp.setRole(role);
        gp.setAlive(alive);
        return gp;
    }

    private Game game(String winningTeam, List<GamePlayer> players) {
        Game g = new Game();
        g.setId(1L);
        g.setCurrentRound(5);
        g.setWinningTeam(winningTeam);
        g.setGamePlayers(new ArrayList<>(players));
        return g;
    }

    @Test
    void didWinReflectsTeamAlignment() {
        assertTrue(eloService.didWin(Role.MAFIA, "MAFIA_WIN"));
        assertTrue(eloService.didWin(Role.MUTILATOR, "MAFIA_WIN"));
        assertTrue(eloService.didWin(Role.SHERIFF, "VILLAGER_WIN"));
        assertTrue(eloService.didWin(Role.JESTER, "JESTER_WIN"));

        assertFalse(eloService.didWin(Role.MAFIA, "VILLAGER_WIN"));
        assertFalse(eloService.didWin(Role.VILLAGER, "MAFIA_WIN"));
        assertFalse(eloService.didWin(null, "VILLAGER_WIN"));
    }

    @Test
    void expectedScoreUsesFallbackWinrates() {
        assertEquals(0.10, eloService.expectedScore(Role.JESTER), 1e-9);
        assertEquals(0.50, eloService.expectedScore(Role.VILLAGER), 1e-9);
        assertEquals(0.45, eloService.expectedScore(Role.MAFIA), 1e-9);
    }

    @Test
    void winningVillagerGainsRating() {
        lenient().when(gameActionRepository.findByGameId(anyLong())).thenReturn(List.of());
        MafiaUser u = user(1);
        GamePlayer gp = player(u, Role.VILLAGER, true);
        Game g = game("VILLAGER_WIN", List.of(gp));

        int delta = eloService.computeDelta(g, gp, 5);
        // K=16, expected=0.5, fără acțiuni => personal=0 => round(0.8*16*0.5)=6
        assertEquals(6, delta);
    }

    @Test
    void losingVillagerLosesRating() {
        lenient().when(gameActionRepository.findByGameId(anyLong())).thenReturn(List.of());
        MafiaUser u = user(1);
        GamePlayer gp = player(u, Role.VILLAGER, false);
        Game g = game("MAFIA_WIN", List.of(gp));

        int delta = eloService.computeDelta(g, gp, 5);
        assertEquals(-6, delta);
    }

    @Test
    void winDeltaIsGreaterThanLossDelta() {
        lenient().when(gameActionRepository.findByGameId(anyLong())).thenReturn(List.of());
        MafiaUser u = user(1);
        GamePlayer winner = player(u, Role.VILLAGER, true);
        GamePlayer loser = player(u, Role.VILLAGER, false);

        int winDelta = eloService.computeDelta(game("VILLAGER_WIN", List.of(winner)), winner, 5);
        int lossDelta = eloService.computeDelta(game("MAFIA_WIN", List.of(loser)), loser, 5);
        assertTrue(winDelta > lossDelta);
    }

    @Test
    void jesterUsesTeamOnlyDelta() {
        MafiaUser u = user(1);
        GamePlayer win = player(u, Role.JESTER, true);
        GamePlayer loss = player(u, Role.JESTER, true);

        // K=24, expected=0.10. Victorie: round(24*0.9)=22. Înfrângere: round(24*-0.1)=-2.
        assertEquals(22, eloService.computeDelta(game("JESTER_WIN", List.of(win)), win, 5));
        assertEquals(-2, eloService.computeDelta(game("VILLAGER_WIN", List.of(loss)), loss, 5));
    }

    @Test
    void personalScoreIsClampedToOne() {
        MafiaUser sheriffUser = user(1);
        MafiaUser mafiaUser = user(2);
        GamePlayer sheriff = player(sheriffUser, Role.SHERIFF, true);
        GamePlayer mafia = player(mafiaUser, Role.MAFIA, true);
        Game g = game("VILLAGER_WIN", List.of(sheriff, mafia));

        // 5 investigații reușite pe un mafiot: 5 * 0.30 = 1.5, trebuie limitat la 1.0
        List<GameAction> actions = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            GameAction a = new GameAction();
            a.setActor(sheriffUser);
            a.setTarget(mafiaUser);
            a.setActionType(GameAction.ActionType.INVESTIGATE);
            a.setRound(5);
            actions.add(a);
        }
        lenient().when(gameActionRepository.findByGameId(anyLong())).thenReturn(actions);

        double score = eloService.computePersonalScore(g, sheriff, 5);
        assertEquals(1.0, score, 1e-9);
    }
}
