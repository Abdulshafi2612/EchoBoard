package com.echoboard.service.impl;

import com.echoboard.dto.session.CreateSessionRequest;
import com.echoboard.dto.session.SessionResponse;
import com.echoboard.entity.Session;
import com.echoboard.entity.User;
import com.echoboard.mapper.SessionMapper;
import com.echoboard.repository.SessionRepository;
import com.echoboard.service.CurrentUserService;
import com.echoboard.service.SessionService;
import com.echoboard.util.AccessCodeGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import static com.echoboard.enums.SessionStatus.SCHEDULED;

@Service
@RequiredArgsConstructor
public class SessionServiceImpl implements SessionService {

    private final SessionRepository sessionRepository;
    private final SessionMapper sessionMapper;
    private final CurrentUserService currentUserService;


    @Override
    public SessionResponse createSession(CreateSessionRequest request) {
        User user = currentUserService.getCurrentUser();
        Session session = sessionMapper.createSessionRequestToSession(request);
        String accessCode = generateUniqueAccessCode();
        session.setAccessCode(accessCode);
        session.setOwner(user);
        session.setStatus(SCHEDULED);

        session.setModerationEnabled(
                request.getModerationEnabled() != null ? request.getModerationEnabled() : true
        );

        session.setAnonymousAllowed(
                request.getAnonymousAllowed() != null ? request.getAnonymousAllowed() : true
        );

        Session savedSession = sessionRepository.save(session);

        return sessionMapper.sessionToSessionResponse(savedSession);
    }

    private String generateUniqueAccessCode() {
        String accessCode = AccessCodeGenerator.generateAccessCode();
        while (sessionRepository.existsByAccessCode(accessCode)) {
            accessCode = AccessCodeGenerator.generateAccessCode();
        }
        return accessCode;
    }
}
