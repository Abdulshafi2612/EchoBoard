package com.echoboard.service;

import com.echoboard.dto.participant.JoinSessionRequest;
import com.echoboard.dto.participant.JoinSessionResponse;
import com.echoboard.entity.Participant;

public interface ParticipantService {

    JoinSessionResponse joinSession(JoinSessionRequest request);

    Participant getParticipantById(Long participantId);

    long getNumberOfTotalParticipantsBySessionId(Long sessionId);
}
