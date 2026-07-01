package com.andreichiri.mafia_backend.service;

import com.andreichiri.mafia_backend.entity.GamePlayer.Role;
import com.andreichiri.mafia_backend.entity.Lobby;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Teste pentru atribuirea rolurilor în funcție de configurația lobby-ului.
 */
class RoleAssignmentServiceTest {

    private final RoleAssignmentService service = new RoleAssignmentService();

    private Lobby lobby(int mafia, boolean sheriff, boolean doctor, boolean jester, boolean mutilator) {
        Lobby l = new Lobby();
        l.setMafiaCount(mafia);
        l.setIncludeSheriff(sheriff);
        l.setIncludeDoctor(doctor);
        l.setIncludeJester(jester);
        l.setIncludeMutilator(mutilator);
        return l;
    }

    private Map<Role, Long> countByRole(List<Role> roles) {
        return roles.stream().collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
    }

    @Test
    void distributesRolesAccordingToConfig() {
        List<Role> roles = service.assignRoles(8, lobby(2, true, true, false, false));
        Map<Role, Long> counts = countByRole(roles);

        assertEquals(8, roles.size());
        assertEquals(2L, counts.get(Role.MAFIA));
        assertEquals(1L, counts.get(Role.SHERIFF));
        assertEquals(1L, counts.get(Role.DOCTOR));
        assertEquals(4L, counts.get(Role.VILLAGER));
        assertNull(counts.get(Role.JESTER));
        assertNull(counts.get(Role.MUTILATOR));
    }

    @Test
    void includesAllSpecialRolesWhenToggled() {
        List<Role> roles = service.assignRoles(8, lobby(1, true, true, true, true));
        Map<Role, Long> counts = countByRole(roles);

        assertEquals(1L, counts.get(Role.MAFIA));
        assertEquals(1L, counts.get(Role.SHERIFF));
        assertEquals(1L, counts.get(Role.DOCTOR));
        assertEquals(1L, counts.get(Role.JESTER));
        assertEquals(1L, counts.get(Role.MUTILATOR));
        assertEquals(3L, counts.get(Role.VILLAGER));
    }

    @Test
    void capsMafiaToMinority() {
        // 4 jucători, dar cerem 5 mafioți: trebuie limitat la 1 (max (4-1)/2)
        List<Role> roles = service.assignRoles(4, lobby(5, false, false, false, false));
        Map<Role, Long> counts = countByRole(roles);

        assertEquals(4, roles.size());
        assertEquals(1L, counts.get(Role.MAFIA));
        assertEquals(3L, counts.get(Role.VILLAGER));
    }

    @Test
    void alwaysAtLeastOneMafia() {
        List<Role> roles = service.assignRoles(6, lobby(0, false, false, false, false));
        assertTrue(countByRole(roles).getOrDefault(Role.MAFIA, 0L) >= 1L);
    }

    @Test
    void totalCountAlwaysMatchesPlayerCount() {
        for (int n = 4; n <= 12; n++) {
            List<Role> roles = service.assignRoles(n, lobby(2, true, true, true, true));
            assertEquals(n, roles.size(), "pentru " + n + " jucători");
        }
    }
}
