package com.echoboard.controller;

import com.echoboard.dto.common.PageResponse;
import com.echoboard.dto.question.QuestionResponse;
import com.echoboard.dto.question.SubmitQuestionRequest;
import com.echoboard.enums.QuestionStatus;
import com.echoboard.service.QuestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static org.springframework.http.HttpStatus.CREATED;

@RestController
@RequestMapping("/api/v1/sessions/{sessionId}/questions")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionService questionService;

    @GetMapping
    public ResponseEntity<PageResponse<QuestionResponse>> getAllQuestions(
            @PathVariable("sessionId") Long sessionId,
            @RequestParam QuestionStatus status,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        PageResponse<QuestionResponse> questions = questionService.getQuestionsBySessionIdAndStatus(pageable, sessionId, status);

        return ResponseEntity.ok(questions);
    }


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
    @PatchMapping("/{questionId}/unpin")
    public ResponseEntity<Void> unpinquestion(
            @PathVariable Long sessionId,
            @PathVariable Long questionId
    ) {
        questionService.unpinquestion(sessionId, questionId);

        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{questionId}/answer")
    public ResponseEntity<Void> markQuestionAsAnswered(
            @PathVariable Long sessionId,
            @PathVariable Long questionId
    ) {
        questionService.markQuestionAsAnswered(sessionId, questionId);

        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{questionId}")
    public ResponseEntity<Void> deleteQuestion(
            @PathVariable Long sessionId,
            @PathVariable Long questionId
    ) {
        questionService.deleteQuestion(sessionId, questionId);
        return ResponseEntity.noContent().build();
    }
}