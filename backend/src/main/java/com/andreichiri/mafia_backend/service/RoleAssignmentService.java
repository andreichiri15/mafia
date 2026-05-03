package com.andreichiri.mafia_backend.service;

import com.andreichiri.mafia_backend.entity.GamePlayer;
import com.andreichiri.mafia_backend.entity.Lobby;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class RoleAssignmentService {

    /**
     * Assigns roles based on lobby configuration:
     * - exactly `mafiaCount` mafia members
     * - at most one of each special role (sheriff, doctor, jester, mutilator) when toggled on
     * - rest are villagers
     * If the configuration somehow produces too many roles for the player count,
     * extras are dropped and replaced with villagers.
     */
    public List<GamePlayer.Role> assignRoles(int playerCount, Lobby lobby) {
        int mafiaCount = lobby.getMafiaCount() != null ? lobby.getMafiaCount() : 1;
        // Hard cap: mafia must be a minority of players at game start
        int maxMafia = Math.max(1, (playerCount - 1) / 2);
        mafiaCount = Math.min(Math.max(1, mafiaCount), maxMafia);

        List<GamePlayer.Role> roles = new ArrayList<>();
        for (int i = 0; i < mafiaCount; i++) roles.add(GamePlayer.Role.MAFIA);
        if (lobby.isIncludeSheriff() && roles.size() < playerCount) roles.add(GamePlayer.Role.SHERIFF);
        if (lobby.isIncludeDoctor() && roles.size() < playerCount) roles.add(GamePlayer.Role.DOCTOR);
        if (lobby.isIncludeJester() && roles.size() < playerCount) roles.add(GamePlayer.Role.JESTER);
        if (lobby.isIncludeMutilator() && roles.size() < playerCount) roles.add(GamePlayer.Role.MUTILATOR);
        while (roles.size() < playerCount) roles.add(GamePlayer.Role.VILLAGER);

        Collections.shuffle(roles);
        return roles;
    }
}
