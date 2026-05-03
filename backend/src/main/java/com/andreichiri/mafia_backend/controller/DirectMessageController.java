package com.andreichiri.mafia_backend.controller;

import com.andreichiri.mafia_backend.dto.DirectMessageDTO;
import com.andreichiri.mafia_backend.security.UserPrincipal;
import com.andreichiri.mafia_backend.service.DirectMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@Controller
public class DirectMessageController {

    @Autowired
    private DirectMessageService directMessageService;

    @GetMapping("/api/dm/{otherUserId}")
    @ResponseBody
    public ResponseEntity<List<DirectMessageDTO.DirectMessageInfo>> getConversation(@PathVariable Long otherUserId) {
        try {
            UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext()
                    .getAuthentication().getPrincipal();
            return ResponseEntity.ok(directMessageService.getConversation(principal.userId(), otherUserId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @MessageMapping("/dm")
    public void handleDirectMessage(
            @Payload DirectMessageDTO.SendDirectMessageBody body,
            Principal principal
    ) {
        UserPrincipal user = extractUser(principal);
        directMessageService.sendMessage(user.userId(), body.receiverId(), body.content());
    }

    private UserPrincipal extractUser(Principal principal) {
        if (principal instanceof UsernamePasswordAuthenticationToken auth) {
            return (UserPrincipal) auth.getPrincipal();
        }
        throw new RuntimeException("Unauthenticated WebSocket connection");
    }
}
