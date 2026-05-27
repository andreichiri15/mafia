package com.andreichiri.mafia_backend.dto;

/**
 * Minimal projection of a friend's identity — only the columns required to
 * render the friends sidebar. Avoids loading the password / email / dateJoined
 * columns of {@code MafiaUser} when listing friendships.
 */
public record FriendBasicView(Long userId, String username) {}
