package com.andreichiri.mafia_backend.repositories;

import com.andreichiri.mafia_backend.entity.MafiaUser;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<MafiaUser, Long> {
    Optional<MafiaUser> findByEmail(String email);
    Optional<MafiaUser> findByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);
    List<MafiaUser> findTop10ByUsernameContainingIgnoreCaseOrderByUsernameAsc(String query);

    /** Top players by ELO for the leaderboard (excludes bots). */
    @Query("SELECT u FROM MafiaUser u WHERE u.isBot = false ORDER BY u.elo DESC")
    List<MafiaUser> findTopByElo(Pageable pageable);
}
