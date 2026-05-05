package com.echoboard.controller;

import com.echoboard.service.PresenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class PresenceController {

    private final PresenceService presenceService;

    @MessageMapping("/sessions/{sessionId}/presence.join")
    public void join(
            @DestinationVariable("sessionId") Long sessionId,
            SimpMessageHeaderAccessor headerAccessor
    ) {
        presenceService.join(
                sessionId,
                headerAccessor.getSessionId(),
                (String) headerAccessor.getSessionAttributes().get("tokenType"),
                (Long) headerAccessor.getSessionAttributes().get("sessionId")
        );
    }

    @MessageMapping("/sessions/presence.leave")
    public void leave(SimpMessageHeaderAccessor headerAccessor) {
        presenceService.leave(headerAccessor.getSessionId());
    }
}