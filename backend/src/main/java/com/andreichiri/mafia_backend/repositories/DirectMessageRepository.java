package com.andreichiri.mafia_backend.repositories;

import com.andreichiri.mafia_backend.entity.DirectMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DirectMessageRepository extends JpaRepository<DirectMessage, Long> {

    @Query("SELECT m FROM DirectMessage m WHERE " +
            "(m.sender.userId = :userA AND m.receiver.userId = :userB) OR " +
            "(m.sender.userId = :userB AND m.receiver.userId = :userA) " +
            "ORDER BY m.sentAt ASC")
    List<DirectMessage> findConversation(Long userA, Long userB);
}
