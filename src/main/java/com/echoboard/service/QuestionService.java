package com.echoboard.service;

import com.echoboard.dto.question.SubmitQuestionRequest;

public interface QuestionService {

    void submitQuestion(Long sessionId, SubmitQuestionRequest request);

    void approveQuestion(Long sessionId, Long questionId);

    void hideQuestion(Long sessionId, Long questionId);

    void pinQuestion(Long sessionId, Long questionId);
}