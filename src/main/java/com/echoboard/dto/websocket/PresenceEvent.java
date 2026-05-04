package com.echoboard.dto.websocket;

import com.echoboard.enums.PresenceEventType;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PresenceEvent {

    private Long sessionId;

    private PresenceEventType type;

    private int onlineCount;
}
