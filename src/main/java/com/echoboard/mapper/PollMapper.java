package com.echoboard.mapper;

import com.echoboard.dto.poll.CreatePollRequest;
import com.echoboard.dto.poll.PollOptionRequest;
import com.echoboard.dto.poll.PollOptionResponse;
import com.echoboard.dto.poll.PollResponse;
import com.echoboard.dto.websocket.PollEvent;
import com.echoboard.entity.Poll;
import com.echoboard.entity.PollOption;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PollMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "session", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "publishedAt", ignore = true)
    @Mapping(target = "closedAt", ignore = true)
    Poll createPollRequestToPoll(CreatePollRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "poll", ignore = true)
    @Mapping(target = "voteCount", ignore = true)
    PollOption pollOptionRequestToPollOption(PollOptionRequest request);

    List<PollOption> pollOptionRequestsToPollOptions(List<PollOptionRequest> requests);

    PollOptionResponse pollOptionToPollOptionResponse(PollOption option);

    List<PollOptionResponse> pollOptionsToPollOptionResponses(List<PollOption> options);

    @Mapping(source = "poll.id", target = "id")
    @Mapping(source = "poll.session.id", target = "sessionId")
    @Mapping(source = "poll.title", target = "title")
    @Mapping(source = "poll.status", target = "status")
    @Mapping(source = "poll.type", target = "type")
    @Mapping(source = "poll.createdAt", target = "createdAt")
    @Mapping(source = "poll.publishedAt", target = "publishedAt")
    @Mapping(source = "poll.closedAt", target = "closedAt")
    @Mapping(source = "options", target = "options")
    PollResponse pollToPollResponse(Poll poll, List<PollOptionResponse> options);


    @Mapping(source = "poll.id", target = "id")
    @Mapping(source = "poll.session.id", target = "sessionId")
    @Mapping(source = "poll.title", target = "title")
    @Mapping(source = "poll.status", target = "status")
    @Mapping(source = "poll.type", target = "type")
    @Mapping(source = "poll.createdAt", target = "createdAt")
    @Mapping(source = "poll.publishedAt", target = "publishedAt")
    @Mapping(source = "poll.closedAt", target = "closedAt")
    @Mapping(source = "options", target = "options")
    @Mapping(target = "eventType", ignore = true)
    PollEvent pollToPollEvent(Poll poll, List<PollOptionResponse> options);
}