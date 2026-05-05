package com.echoboard.service;

import org.springframework.messaging.simp.SimpMessageHeaderAccessor;

public interface PresenceService {

    void disconnect(String webSocketSessionId);

    void join(Long sessionId, String webSocketSessionId, String tokenType, Long tokenSessionId);

    void leave(Long sessionId);


}
