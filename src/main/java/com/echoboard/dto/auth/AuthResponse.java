package com.echoboard.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {

    private String accessToken;

    private String refreshToken;

    private String tokenType="Bearer";

    public AuthResponse(String accessToken, String refreshToken) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }
}