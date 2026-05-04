package com.echoboard.config;

import com.echoboard.service.PresenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
@Slf4j
@RequiredArgsConstructor
public class WebSocketEventListener {

    private final PresenceService presenceService;

    @EventListener
    public void handleConnect(SessionConnectedEvent event) {
        System.out.println("WebSocket connected");
    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        log.info("WebSocket disconnected: {}", event.getSessionId());
        presenceService.disconnect(event.getSessionId());
    }
}