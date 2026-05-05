package com.echoboard.controller;

import com.echoboard.dto.question.SubmitQuestionRequest;
import com.echoboard.service.QuestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.HttpStatus.CREATED;

@RestController
@RequestMapping("/api/v1/sessions/{sessionId}/questions")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionService questionService;

    @PostMapping
    public ResponseEntity<Void> submitQuestion(
            @PathVariable Long sessionId,
            @Valid @RequestBody SubmitQuestionRequest request
    ) {
        questionService.submitQuestion(sessionId, request);

        return  ResponseEntity.status(CREATED).build();
    }
}