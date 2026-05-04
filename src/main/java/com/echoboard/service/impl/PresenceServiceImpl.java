package com.echoboard.service.impl;

import com.echoboard.dto.websocket.PresenceEvent;
import com.echoboard.enums.PresenceEventType;
import com.echoboard.service.PresenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.echoboard.enums.PresenceEventType.JOINED;
import static com.echoboard.enums.PresenceEventType.LEFT;

@Service
@RequiredArgsConstructor
public class PresenceServiceImpl implements PresenceService {

    private final Map<Long, Integer> onlineCounts = new ConcurrentHashMap<>();
    private final Map<String, Long> webSocketSessionToSessionId = new ConcurrentHashMap<>();

    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void join(Long sessionId, String webSocketSessionId) {
        if (webSocketSessionToSessionId.containsKey(webSocketSessionId)) {
            return;
        }
        webSocketSessionToSessionId.put(webSocketSessionId, sessionId);
        int count = onlineCounts.merge(sessionId, 1, Integer::sum);
        broadcastPresenceEvent(sessionId, count, JOINED);
    }

    @Override
    public void leave(Long sessionId) {
        Integer count = onlineCounts.computeIfPresent(sessionId, (key, currentCount) -> {
            int newCount = currentCount - 1;
            return newCount <= 0 ? null : newCount;

        });

        count = count == null ? 0 : count;
        broadcastPresenceEvent(sessionId, count, LEFT);
    }

    @Override
    public void disconnect(String webSocketSessionId) {
        Long sessionId = webSocketSessionToSessionId.remove(webSocketSessionId);

        if (sessionId == null) {
            return;
        }

        leave(sessionId);
    }

    private void broadcastPresenceEvent(Long sessionId, int count, PresenceEventType type) {
        PresenceEvent event = PresenceEvent
                .builder()
                .sessionId(sessionId)
                .type(type)
                .onlineCount(count)
                .build();
        messagingTemplate.convertAndSend(
                "/topic/sessions/" + sessionId + "/presence",
                event
        );
    }

}
