package com.echoboard.controller;

import com.echoboard.dto.session.CreateSessionRequest;
import com.echoboard.dto.session.SessionResponse;
import com.echoboard.service.SessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/sessions")
public class SessionController {

    private final SessionService sessionService;

    @PostMapping
    public ResponseEntity<SessionResponse> createSession(
            @Valid @RequestBody CreateSessionRequest request) {

        SessionResponse sessionResponse = sessionService.createSession(request);
        return new ResponseEntity<>(sessionResponse, HttpStatus.CREATED);
    }
}
