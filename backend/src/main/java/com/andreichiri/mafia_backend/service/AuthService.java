package com.andreichiri.mafia_backend.service;

import com.andreichiri.mafia_backend.dto.AuthResponse;
import com.andreichiri.mafia_backend.dto.LoginRequest;
import com.andreichiri.mafia_backend.dto.RegisterRequest;
import com.andreichiri.mafia_backend.entity.MafiaUser;
import com.andreichiri.mafia_backend.repositories.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public ResponseEntity<?> login(LoginRequest request) {
        MafiaUser user = userRepository.findByEmail(request.email())
                .orElse(null);

        if (user == null || !passwordEncoder.matches(request.password(), user.getPassword())) {
            return ResponseEntity.status(401).body("Invalid email or password");
        }

        String token = jwtTokenProvider.generateToken(user.getUsername(), user.getUserId());
        return ResponseEntity.ok(new AuthResponse(token, user.getUserId(), user.getUsername()));
    }

    public ResponseEntity<?> register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            return ResponseEntity.badRequest().body("Email already in use");
        }

        if (userRepository.existsByUsername(request.username())) {
            return ResponseEntity.badRequest().body("Username already taken");
        }

        MafiaUser user = new MafiaUser();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));

        userRepository.save(user);

        String token = jwtTokenProvider.generateToken(user.getUsername(), user.getUserId());
        return ResponseEntity.ok(new AuthResponse(token, user.getUserId(), user.getUsername()));
    }
}
