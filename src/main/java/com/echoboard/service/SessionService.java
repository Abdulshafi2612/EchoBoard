package com.echoboard.service;

import com.echoboard.dto.session.CreateSessionRequest;
import com.echoboard.dto.session.SessionResponse;

public interface SessionService {

    SessionResponse createSession(CreateSessionRequest request);
}
