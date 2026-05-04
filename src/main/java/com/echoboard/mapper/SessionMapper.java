package com.echoboard.mapper;

import com.echoboard.dto.session.CreateSessionRequest;
import com.echoboard.dto.session.SessionResponse;
import com.echoboard.dto.session.UpdateSessionRequest;
import com.echoboard.entity.Session;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SessionMapper {

    @Mapping(target = "id" , ignore = true)
    @Mapping(target = "accessCode" , ignore = true)
    @Mapping(target = "status" , ignore = true)
    @Mapping(target = "owner" , ignore = true)
    @Mapping(target = "createdAt" , ignore = true)
    @Mapping(target = "startedAt" , ignore = true)
    @Mapping(target = "endedAt" , ignore = true)
    Session createSessionRequestToSession(CreateSessionRequest request);


    @Mapping(target = "ownerName", source = "owner.name")
    @Mapping(target = "ownerId", source = "owner.id")
    SessionResponse sessionToSessionResponse(Session session);
}
