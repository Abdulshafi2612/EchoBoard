package com.echoboard.controller;

import com.echoboard.dto.common.PageResponse;
import com.echoboard.dto.session.CreateSessionRequest;
import com.echoboard.dto.session.SessionResponse;
import com.echoboard.service.SessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("/my")
    public ResponseEntity<PageResponse<SessionResponse>> getMySessions(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        PageResponse<SessionResponse> response = sessionService.getMySessions(pageable);
        return ResponseEntity.ok(response);
    }
}
