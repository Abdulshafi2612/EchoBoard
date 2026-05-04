package com.echoboard.mapper;

import com.echoboard.dto.participant.JoinSessionRequest;
import com.echoboard.dto.participant.JoinSessionResponse;
import com.echoboard.entity.Participant;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ParticipantMapper {



    @Mapping(target = "id", ignore = true)
    @Mapping(target = "session", ignore = true)
    @Mapping(target = "participantTokenHash", ignore = true)
    @Mapping(target = "mutedUntil", ignore = true)
    @Mapping(target = "joinedAt", ignore = true)
    @Mapping(target = "lastSeenAt", ignore = true)
    Participant joinParticipantRequestToParticipant(JoinSessionRequest request);

    @Mapping(target = "sessionId" , source = "session.id")
    @Mapping(target = "participantToken" ,ignore = true)
    JoinSessionResponse participantToJoinSessionResponse(Participant participant);

}
