package com.andreichiri.mafia_backend.security;

import java.security.Principal;

public record UserPrincipal(Long userId, String username) implements Principal {
    @Override
    public String getName() {
        return userId.toString();
    }
}
