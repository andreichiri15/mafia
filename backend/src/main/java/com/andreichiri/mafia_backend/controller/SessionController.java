package com.andreichiri.mafia_backend.controller;

import com.andreichiri.mafia_backend.dto.SessionDTO;
import com.andreichiri.mafia_backend.security.UserPrincipal;
import com.andreichiri.mafia_backend.service.SessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sessions")
public class SessionController {

    @Autowired
    private SessionService sessionService;

    @GetMapping("/me")
    public ResponseEntity<SessionDTO.SessionInfo> getMySession() {
        UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        SessionDTO.SessionInfo info = sessionService.getSession(principal.userId());
        // Return 200 with null body when no session — avoids treating "no session" as an error
        return ResponseEntity.ok(info);
    }
}
