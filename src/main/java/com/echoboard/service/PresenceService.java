package com.echoboard.service;

public interface PresenceService {

    void disconnect(String webSocketSessionId);

    void join(Long sessionId, String webSocketSessionId, String tokenType, Long tokenSessionId);

    void leave(String webSocketSessionId);


}
