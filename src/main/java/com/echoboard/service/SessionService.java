package com.echoboard.service;

import com.echoboard.dto.common.PageResponse;
import com.echoboard.dto.session.CreateSessionRequest;
import com.echoboard.dto.session.SessionResponse;
import org.springframework.data.domain.Pageable;

public interface SessionService {

    SessionResponse createSession(CreateSessionRequest request);

    PageResponse<SessionResponse> getMySessions(Pageable pageable);
}
