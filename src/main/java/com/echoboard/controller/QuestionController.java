package com.echoboard.controller;

import com.echoboard.dto.question.SubmitQuestionRequest;
import com.echoboard.service.QuestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionService questionService;

    @MessageMapping("/sessions/{sessionId}/questions.submit")
    public void submitQuestion(@Valid SubmitQuestionRequest request,
                               @DestinationVariable("sessionId") Long sessionId,
                               SimpMessageHeaderAccessor headerAccessor) {

        questionService.submitQuestion(
                sessionId,
                (Long) headerAccessor.getSessionAttributes().get("participantId"),
                request
        );

    }
}
