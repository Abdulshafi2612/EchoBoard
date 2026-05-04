package com.echoboard.service;

public interface PresenceService {

    void disconnect(String webSocketSessionId);

    void join(Long sessionId, String webSocketSessionId);

    void leave(Long sessionId);


}
