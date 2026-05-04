package com.echoboard.service.impl;

import com.echoboard.dto.common.PageResponse;
import com.echoboard.dto.session.CreateSessionRequest;
import com.echoboard.dto.session.SessionResponse;
import com.echoboard.entity.Session;
import com.echoboard.entity.User;
import com.echoboard.exception.AppException;
import com.echoboard.exception.ErrorCode;
import com.echoboard.mapper.SessionMapper;
import com.echoboard.repository.SessionRepository;
import com.echoboard.service.CurrentUserService;
import com.echoboard.service.SessionService;
import com.echoboard.util.AccessCodeGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.echoboard.enums.SessionStatus.SCHEDULED;
import static org.springframework.http.HttpStatus.NOT_FOUND;

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

    @Override
    public PageResponse<SessionResponse> getMySessions(Pageable pageable) {
        User user = currentUserService.getCurrentUser();

        Page<Session> sessionsPage = sessionRepository.findByOwner(user, pageable);

        List<SessionResponse> content = sessionsPage
                .getContent()
                .stream()
                .map(sessionMapper::sessionToSessionResponse)
                .toList();

        return new PageResponse<>(
                content,
                sessionsPage.getNumber(),
                sessionsPage.getSize(),
                sessionsPage.getTotalElements(),
                sessionsPage.getTotalPages(),
                sessionsPage.isLast()
        );

    }

    @Override
    public SessionResponse getSessionById(Long id) {
        User owner = currentUserService.getCurrentUser();
        Session session = sessionRepository.findByIdAndOwner(id, owner)
                .orElseThrow(() -> new AppException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        NOT_FOUND,
                        "Session not found"));
        return sessionMapper.sessionToSessionResponse(session);
    }

    private String generateUniqueAccessCode() {
        String accessCode = AccessCodeGenerator.generateAccessCode();
        while (sessionRepository.existsByAccessCode(accessCode)) {
            accessCode = AccessCodeGenerator.generateAccessCode();
        }
        return accessCode;
    }
}
