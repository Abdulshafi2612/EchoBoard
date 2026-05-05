package com.echoboard.service;

import com.echoboard.dto.question.SubmitQuestionRequest;

public interface QuestionService {

    void submitQuestion(Long sessionId, Long participantId, SubmitQuestionRequest request);
}
