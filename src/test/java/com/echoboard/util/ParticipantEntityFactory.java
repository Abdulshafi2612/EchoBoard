package com.echoboard.util;

import com.echoboard.entity.Participant;
import com.echoboard.entity.Session;

import java.time.LocalDateTime;

public final class ParticipantEntityFactory {

    private ParticipantEntityFactory() {
    }

    public static Participant participant() {
        return participantForSession(SessionEntityFactory.liveSession());
    }

    public static Participant participantForSession(Session session) {
        Participant participant = new Participant();

        participant.setId(1L);
        participant.setSession(session);
        participant.setDisplayName("Test Participant");
        participant.setParticipantTokenHash("participant-token-hash");
        participant.setMutedUntil(null);
        participant.setJoinedAt(LocalDateTime.now().minusMinutes(20));
        participant.setLastSeenAt(LocalDateTime.now());

        return participant;
    }

    public static Participant anonymousParticipantForSession(Session session) {
        Participant participant = participantForSession(session);

        participant.setDisplayName("Anonymous");

        return participant;
    }

    public static Participant mutedParticipantForSession(Session session) {
        Participant participant = participantForSession(session);

        participant.setMutedUntil(LocalDateTime.now().plusMinutes(10));

        return participant;
    }
}