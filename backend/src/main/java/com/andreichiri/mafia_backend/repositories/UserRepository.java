package com.andreichiri.mafia_backend.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.andreichiri.mafia_backend.entity.MafiaUser;

@Repository
public interface UserRepository extends JpaRepository<Integer, MafiaUser>{
    
    
}
