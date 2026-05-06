package com.echoboard.controller;

import com.echoboard.dto.poll.CreatePollRequest;
import com.echoboard.dto.poll.PollResponse;
import com.echoboard.service.PollService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/sessions/{sessionId}/polls")
public class PollController {

    private final PollService pollService;


    @PostMapping
    public ResponseEntity<PollResponse> submitDraftPoll(@Valid @RequestBody CreatePollRequest request,
                                                        @PathVariable Long sessionId) {
        PollResponse pollResponse = pollService.submitDraftPoll(request, sessionId);

        return ResponseEntity.status(HttpStatus.CREATED).body(pollResponse);

    }

    @PatchMapping("/{pollId}/publish")
    public ResponseEntity<PollResponse> publishPoll(@PathVariable Long pollId,
                                                    @PathVariable Long sessionId) {
        PollResponse pollResponse = pollService.publishPoll(pollId, sessionId);

        return ResponseEntity.ok(pollResponse);

    }

    @PostMapping("/{pollId}/vote/{optionId}")
    public ResponseEntity<PollResponse> voteOnPoll(@PathVariable Long pollId,
                                                   @PathVariable Long sessionId,
                                                   @PathVariable Long optionId) {
        PollResponse pollResponse = pollService.voteOnPoll(pollId, sessionId, optionId);

        return ResponseEntity.ok(pollResponse);

    }
}
