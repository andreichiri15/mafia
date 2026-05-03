package com.andreichiri.mafia_backend.controller;

import com.andreichiri.mafia_backend.dto.FriendDTO;
import com.andreichiri.mafia_backend.security.UserPrincipal;
import com.andreichiri.mafia_backend.service.FriendService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/friends")
public class FriendController {

    @Autowired
    private FriendService friendService;

    @GetMapping
    public ResponseEntity<List<FriendDTO.FriendInfo>> listFriends() {
        return ResponseEntity.ok(friendService.listFriends(getUserId()));
    }

    @GetMapping("/requests")
    public ResponseEntity<List<FriendDTO.FriendRequestInfo>> listRequests() {
        return ResponseEntity.ok(friendService.listIncomingRequests(getUserId()));
    }

    @PostMapping("/request")
    public ResponseEntity<?> sendRequest(@RequestBody FriendDTO.SendRequestBody body) {
        try {
            friendService.sendRequest(getUserId(), body.username());
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{requestId}/accept")
    public ResponseEntity<?> accept(@PathVariable Long requestId) {
        try {
            friendService.acceptRequest(getUserId(), requestId);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{requestId}/decline")
    public ResponseEntity<?> decline(@PathVariable Long requestId) {
        try {
            friendService.declineRequest(getUserId(), requestId);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{friendUserId}")
    public ResponseEntity<?> removeFriend(@PathVariable Long friendUserId) {
        try {
            friendService.removeFriend(getUserId(), friendUserId);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    private Long getUserId() {
        UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        return principal.userId();
    }
}
