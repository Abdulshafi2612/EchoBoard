package com.echoboard.service;

import com.echoboard.dto.common.PageResponse;
import com.echoboard.dto.session.CreateSessionRequest;
import com.echoboard.dto.session.SessionResponse;
import com.echoboard.dto.session.UpdateSessionRequest;
import com.echoboard.entity.Session;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface SessionService {

    SessionResponse createSession(CreateSessionRequest request);

    PageResponse<SessionResponse> getMySessions(Pageable pageable);

    SessionResponse getSessionResponseById(Long id);

    Session getSessionById(Long id);

    Session getSessionByAccessCode(String accessCode);

    SessionResponse updateSession(Long id, UpdateSessionRequest request);

    SessionResponse startSession(Long id);

    SessionResponse endSession(Long id);

    SessionResponse archiveSession(Long id);

    void deleteSession(Long id);

    Session getOwnedSessionOrThrow(Long sessionId);

    SessionResponse uploadSessionLogo(Long sessionId, MultipartFile file);
}
