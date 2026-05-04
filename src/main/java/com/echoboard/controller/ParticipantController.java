package com.echoboard.controller;

import com.echoboard.dto.participant.JoinSessionRequest;
import com.echoboard.dto.participant.JoinSessionResponse;
import com.echoboard.service.ParticipantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/sessions")
@RequiredArgsConstructor
public class ParticipantController {

    private final ParticipantService participantService;

    @PostMapping("/join")
    public ResponseEntity<JoinSessionResponse> joinSession(
            @Valid @RequestBody JoinSessionRequest request
    ) {
        JoinSessionResponse response = participantService.joinSession(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}