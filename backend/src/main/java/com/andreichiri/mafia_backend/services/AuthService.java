package com.andreichiri.mafia_backend.services;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    public ResponseEntity<?> authLogin() {
        return ResponseEntity.ok("This endpoint does not require authentication");
    }

    public ResponseEntity<?> authRegister() {
        return ResponseEntity.ok("This endpoint also does not require authentication :)");
    }
}
