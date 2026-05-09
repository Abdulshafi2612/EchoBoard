package com.echoboard.dto.session;

import com.echoboard.enums.SessionStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SessionResponse {

    private Long id;

    private String title;

    private String description;

    private String accessCode;

    private SessionStatus status;

    private boolean moderationEnabled;

    private boolean anonymousAllowed;

    private String ownerName;

    private Long ownerId;

    private String logoUrl;

    private LocalDateTime createdAt;

    private LocalDateTime startedAt;

    private LocalDateTime endedAt;
}
