package com.andreichiri.mafia_backend.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.andreichiri.mafia_backend.entity.MafiaUser;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<MafiaUser, Long> {
    Optional<MafiaUser> findByEmail(String email);
    Optional<MafiaUser> findByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);
    java.util.List<MafiaUser> findTop10ByUsernameContainingIgnoreCaseOrderByUsernameAsc(String query);
}
