package com.echoboard.dto.auth;

import com.echoboard.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RegisterResponse {

    private Long id;

    private String name;

    private String email;

    private UserRole role;

    private String message;
}