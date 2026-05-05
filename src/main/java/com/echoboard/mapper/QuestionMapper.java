package com.echoboard.mapper;

import com.echoboard.dto.websocket.QuestionEvent;
import com.echoboard.entity.Question;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface QuestionMapper {

    @Mapping(target = "questionId", source = "id")
    @Mapping(target = "sessionId", source = "session.id")
    @Mapping(target = "participantDisplayName", source = "participant.displayName")
    QuestionEvent questionToQuestionEvent(Question question);
}