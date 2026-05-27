package com.andreichiri.mafia_backend.repositories;

import com.andreichiri.mafia_backend.dto.FriendBasicView;
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

    /**
     * Returns just the (userId, username) of each accepted friend, in a single
     * JOIN query. Avoids the per-friend lazy-load of MafiaUser (which would
     * otherwise pull password/email/dateJoined too).
     */
    @Query("SELECT new com.andreichiri.mafia_backend.dto.FriendBasicView( " +
            "  CASE WHEN f.requester.userId = :userId THEN f.addressee.userId ELSE f.requester.userId END, " +
            "  CASE WHEN f.requester.userId = :userId THEN f.addressee.username ELSE f.requester.username END " +
            ") FROM Friendship f WHERE " +
            "(f.requester.userId = :userId OR f.addressee.userId = :userId) AND f.status = 'ACCEPTED'")
    List<FriendBasicView> findAcceptedFriendsBasic(Long userId);

    @Query("SELECT f FROM Friendship f WHERE f.addressee.userId = :userId AND f.status = 'PENDING'")
    List<Friendship> findIncomingPending(Long userId);

    @Query("SELECT f FROM Friendship f WHERE f.requester.userId = :userId AND f.status = 'PENDING'")
    List<Friendship> findOutgoingPending(Long userId);

    @Query("SELECT f FROM Friendship f WHERE " +
            "((f.requester.userId = :userA AND f.addressee.userId = :userB) OR " +
            " (f.requester.userId = :userB AND f.addressee.userId = :userA))")
    Optional<Friendship> findBetween(Long userA, Long userB);
}
