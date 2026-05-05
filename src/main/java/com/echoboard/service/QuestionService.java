package com.echoboard.service;

import com.echoboard.dto.question.SubmitQuestionRequest;

public interface QuestionService {

    void submitQuestion(Long sessionId, SubmitQuestionRequest request);
}
