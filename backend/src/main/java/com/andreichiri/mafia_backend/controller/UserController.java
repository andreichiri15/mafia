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
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private FriendService friendService;

    @GetMapping("/search")
    public ResponseEntity<List<FriendDTO.UserSearchResult>> search(@RequestParam("q") String query) {
        UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        return ResponseEntity.ok(friendService.searchUsers(query, principal.userId()));
    }
}
