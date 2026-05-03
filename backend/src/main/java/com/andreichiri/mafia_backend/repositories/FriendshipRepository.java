package com.andreichiri.mafia_backend.repositories;

import com.andreichiri.mafia_backend.entity.Friendship;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

    @Query("SELECT f FROM Friendship f WHERE " +
            "(f.requester.userId = :userId OR f.addressee.userId = :userId) AND f.status = 'ACCEPTED'")
    List<Friendship> findAcceptedByUser(Long userId);

    @Query("SELECT f FROM Friendship f WHERE f.addressee.userId = :userId AND f.status = 'PENDING'")
    List<Friendship> findIncomingPending(Long userId);

    @Query("SELECT f FROM Friendship f WHERE f.requester.userId = :userId AND f.status = 'PENDING'")
    List<Friendship> findOutgoingPending(Long userId);

    @Query("SELECT f FROM Friendship f WHERE " +
            "((f.requester.userId = :userA AND f.addressee.userId = :userB) OR " +
            " (f.requester.userId = :userB AND f.addressee.userId = :userA))")
    Optional<Friendship> findBetween(Long userA, Long userB);
}
