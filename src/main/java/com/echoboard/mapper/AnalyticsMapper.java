package com.echoboard.mapper;

import com.echoboard.dto.analytics.SessionAnalyticsResponse;
import com.echoboard.dto.question.QuestionResponse;
import com.echoboard.entity.Session;
import com.echoboard.entity.SessionAnalyticsSnapshot;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AnalyticsMapper {

    @Mapping(source = "session.id", target = "sessionId")
    @Mapping(source = "session.title", target = "sessionTitle")
    @Mapping(source = "topQuestions", target = "topQuestions")
    SessionAnalyticsResponse snapshotToSessionAnalyticsResponse(
            SessionAnalyticsSnapshot snapshot,
            Session session,
            List<QuestionResponse> topQuestions
    );
}