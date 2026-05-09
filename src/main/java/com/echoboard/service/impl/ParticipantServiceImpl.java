package com.echoboard.service.impl;

import com.echoboard.dto.participant.JoinSessionRequest;
import com.echoboard.dto.participant.JoinSessionResponse;
import com.echoboard.entity.Participant;
import com.echoboard.entity.Session;
import com.echoboard.exception.AppException;
import com.echoboard.exception.ErrorCode;
import com.echoboard.mapper.ParticipantMapper;
import com.echoboard.repository.ParticipantRepository;
import com.echoboard.security.JwtService;
import com.echoboard.service.ParticipantService;
import com.echoboard.service.SessionAccessCodeCacheService;
import com.echoboard.service.SessionService;
import com.echoboard.util.TokenHashUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.echoboard.enums.SessionStatus.LIVE;
import static com.echoboard.exception.ErrorCode.VALIDATION_ERROR;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Service
@RequiredArgsConstructor
public class ParticipantServiceImpl implements ParticipantService {

    private final ParticipantRepository participantRepository;
    private final ParticipantMapper participantMapper;
    private final JwtService jwtService;
    private final SessionAccessCodeCacheService sessionAccessCodeCacheService;

    @Override
    @Transactional
    public JoinSessionResponse joinSession(JoinSessionRequest request) {
        String accessCode = request.getAccessCode().trim().toUpperCase();
        Participant participant = participantMapper.joinParticipantRequestToParticipant(request);

        Session session = sessionAccessCodeCacheService
                .getSessionByAccessCode(accessCode);

        if (session.getStatus() != LIVE) {
            throw new AppException(
                    ErrorCode.INVALID_SESSION_STATUS,
                    BAD_REQUEST,
                    "Only live sessions can be joined");
        }
        participant.setSession(session);

        boolean displayNameMissing =
                request.getDisplayName() == null || request.getDisplayName().isBlank();

        if (!session.isAnonymousAllowed() && displayNameMissing)
            throw new AppException(
                    VALIDATION_ERROR,
                    BAD_REQUEST,
                    "Display name is required for this session");
        else
            participant.setDisplayName(!displayNameMissing
                    ? request.getDisplayName().trim() : "Anonymous");

        Participant savedParticipant = participantRepository.save(participant);

        String participantToken = jwtService.generateParticipantToken(savedParticipant);

        String participantTokenHash = TokenHashUtil.sha256(participantToken);

        savedParticipant.setParticipantTokenHash(participantTokenHash);
        Participant updatedParticipant = participantRepository.save(savedParticipant);

        JoinSessionResponse joinSessionResponse = participantMapper.participantToJoinSessionResponse(updatedParticipant);
        joinSessionResponse.setParticipantToken(participantToken);

        return joinSessionResponse;
    }

    @Override
    public Participant getParticipantById(Long participantId) {
        return participantRepository.findById(participantId).orElse(null);
    }

    @Override
    public long getNumberOfTotalParticipantsBySessionId(Long sessionId) {
        return participantRepository.countBySession_Id(sessionId);
    }
}
