package com.echoboard.service;

import com.echoboard.dto.participant.JoinSessionRequest;
import com.echoboard.dto.participant.JoinSessionResponse;

public interface ParticipantService {

    JoinSessionResponse joinSession(JoinSessionRequest request);

}
