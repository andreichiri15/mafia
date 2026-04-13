package com.andreichiri.mafia_backend.security;

public record UserPrincipal (
    Long userId,
    String username
){}
