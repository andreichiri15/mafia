package com.andreichiri.mafia_backend.dto;

public record AuthResponse(String token, Long userId, String username) {}
