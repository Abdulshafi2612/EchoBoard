package com.echoboard.controller;

import com.echoboard.dto.user.UserProfileResponse;
import com.echoboard.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final AuthService authService;

    @GetMapping("/me")
    ResponseEntity<UserProfileResponse> getCurrentUser() {
        UserProfileResponse response =
                authService.getUserProfileResponse();
        return ResponseEntity.ok(response);
    }
}
