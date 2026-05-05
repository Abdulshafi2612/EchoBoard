package com.echoboard.controller;

import com.echoboard.dto.question.SubmitQuestionRequest;
import com.echoboard.service.QuestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

        return ResponseEntity.status(CREATED).build();
    }

    @PatchMapping("/{questionId}/approve")
    public ResponseEntity<Void> approveQuestion(
            @PathVariable Long sessionId,
            @PathVariable Long questionId
    ) {
        questionService.approveQuestion(sessionId, questionId);

        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{questionId}/hide")
    public ResponseEntity<Void> hideQuestion(
            @PathVariable Long sessionId,
            @PathVariable Long questionId
    ) {
        questionService.hideQuestion(sessionId, questionId);

        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{questionId}/pin")
    public ResponseEntity<Void> pinQuestion(
            @PathVariable Long sessionId,
            @PathVariable Long questionId
    ) {
        questionService.pinQuestion(sessionId, questionId);

        return ResponseEntity.ok().build();
    }
}