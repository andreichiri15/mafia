package com.andreichiri.mafia_backend.controller;

import com.andreichiri.mafia_backend.dto.RankedDTO;
import com.andreichiri.mafia_backend.repositories.UserRepository;
import com.andreichiri.mafia_backend.security.UserPrincipal;
import com.andreichiri.mafia_backend.service.MatchmakingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ranked")
public class RankedController {

    @Autowired
    private MatchmakingService matchmakingService;
    @Autowired
    private UserRepository userRepository;

    @PostMapping("/queue")
    public ResponseEntity<?> joinQueue() {
        try {
            matchmakingService.enqueue(currentUserId());
            return ResponseEntity.ok(matchmakingService.statusFor(currentUserId()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/queue")
    public ResponseEntity<?> leaveQueue() {
        matchmakingService.dequeue(currentUserId());
        return ResponseEntity.ok(matchmakingService.statusFor(currentUserId()));
    }

    @GetMapping("/queue/status")
    public ResponseEntity<RankedDTO.QueueStatus> status() {
        return ResponseEntity.ok(matchmakingService.statusFor(currentUserId()));
    }

    @GetMapping("/leaderboard")
    public ResponseEntity<List<RankedDTO.LeaderboardEntry>> leaderboard() {
        var users = userRepository.findTopByElo(PageRequest.of(0, 50));
        return ResponseEntity.ok(users.stream()
                .map(u -> new RankedDTO.LeaderboardEntry(u.getUserId(), u.getUsername(), u.getElo()))
                .toList());
    }

    private Long currentUserId() {
        UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        return principal.userId();
    }
}
