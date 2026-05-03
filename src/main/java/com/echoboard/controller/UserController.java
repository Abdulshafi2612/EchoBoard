package com.echoboard.controller;

import com.echoboard.dto.user.UserProfileResponse;
import com.echoboard.mapper.UserMapper;
import com.echoboard.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserMapper userMapper;

    @GetMapping("/me")
    ResponseEntity<UserProfileResponse> getCurrentUser(@AuthenticationPrincipal CustomUserDetails currentUser){
        UserProfileResponse response =
                userMapper.userToUserProfileResponse(currentUser.getUser());

        return ResponseEntity.ok(response);
    }
}
