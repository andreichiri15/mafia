package com.andreichiri.mafia_backend.service;

import com.andreichiri.mafia_backend.entity.GamePlayer;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class RoleAssignmentService {

    /**
     * Assigns roles based on player count:
     * - 1 mafia per 3-4 players (minimum 1)
     * - 1 sheriff always
     * - rest are villagers
     */
    public List<GamePlayer.Role> assignRoles(int playerCount) {
        int mafiaCount = Math.max(1, playerCount / 4);
        int sheriffCount = 1;
        int villagerCount = playerCount - mafiaCount - sheriffCount;

        List<GamePlayer.Role> roles = new ArrayList<>();
        for (int i = 0; i < mafiaCount; i++) {
            roles.add(GamePlayer.Role.MAFIA);
        }
        for (int i = 0; i < sheriffCount; i++) {
            roles.add(GamePlayer.Role.SHERIFF);
        }
        for (int i = 0; i < villagerCount; i++) {
            roles.add(GamePlayer.Role.VILLAGER);
        }

        Collections.shuffle(roles);
        return roles;
    }
}
