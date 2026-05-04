package com.echoboard.dto.participant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class JoinSessionResponse {

    private Long id;

    private Long sessionId;

    private String displayName;

    private String participantToken;

    private LocalDateTime joinedAt;

}
