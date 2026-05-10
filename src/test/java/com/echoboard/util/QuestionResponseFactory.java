package com.echoboard.util;

import com.echoboard.dto.question.QuestionResponse;
import com.echoboard.enums.QuestionStatus;

import java.time.LocalDateTime;

public final class QuestionResponseFactory {

    private QuestionResponseFactory() {
    }

    public static QuestionResponse approvedQuestionResponse() {
        return questionResponseWithStatus(QuestionStatus.APPROVED);
    }

    public static QuestionResponse answeredQuestionResponse() {
        QuestionResponse response = questionResponseWithStatus(QuestionStatus.ANSWERED);

        response.setAnswered(true);
        response.setAnsweredAt(LocalDateTime.now().minusMinutes(5));

        return response;
    }

    public static QuestionResponse questionResponseWithStatus(QuestionStatus status) {
        QuestionResponse response = new QuestionResponse();

        response.setId(1L);
        response.setSessionId(1L);
        response.setParticipantDisplayName("Test Participant");
        response.setContent("What is the roadmap?");
        response.setStatus(status);
        response.setUpvoteCount(5);
        response.setPinned(false);
        response.setAnswered(false);
        response.setCreatedAt(LocalDateTime.now().minusMinutes(20));

        if (status == QuestionStatus.APPROVED || status == QuestionStatus.ANSWERED) {
            response.setApprovedAt(LocalDateTime.now().minusMinutes(15));
        }

        return response;
    }
}