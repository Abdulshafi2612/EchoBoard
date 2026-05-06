package com.echoboard.service.impl;

import com.echoboard.dto.poll.CreatePollRequest;
import com.echoboard.dto.poll.PollOptionResponse;
import com.echoboard.dto.poll.PollResponse;
import com.echoboard.dto.websocket.PollEvent;
import com.echoboard.entity.Poll;
import com.echoboard.entity.PollOption;
import com.echoboard.entity.Session;
import com.echoboard.entity.User;
import com.echoboard.enums.PollEventType;
import com.echoboard.exception.AppException;
import com.echoboard.mapper.PollMapper;
import com.echoboard.repository.PollOptionRepository;
import com.echoboard.repository.PollRepository;
import com.echoboard.repository.PollVoteRepository;
import com.echoboard.service.CurrentParticipantService;
import com.echoboard.service.CurrentUserService;
import com.echoboard.service.PollService;
import com.echoboard.service.SessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.echoboard.enums.SessionStatus.LIVE;
import static com.echoboard.enums.SessionStatus.SCHEDULED;
import static com.echoboard.exception.ErrorCode.*;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
@Transactional
public class PollServiceImpl implements PollService {

    private static final String POLLS_TOPIC_TEMPLATE = "/topic/sessions/%d/polls";

    private final PollRepository pollRepository;
    private final PollOptionRepository pollOptionRepository;
    private final PollVoteRepository pollVoteRepository;
    private final PollMapper pollMapper;
    private final CurrentUserService currentUserService;
    private final CurrentParticipantService currentParticipantService;
    private final SessionService sessionService;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public PollResponse submitDraftPoll(CreatePollRequest request, Long sessionId) {
        User user = currentUserService.getCurrentUser();
        Session session = getLiveOrScheduledSessionOrThrow(sessionId);
        validateUserOwnsSession(session, user);
        Poll poll = pollMapper.createPollRequestToPoll(request);
        poll.setSession(session);

        Poll savedPoll = pollRepository.save(poll);
        List<PollOption> options = pollMapper.pollOptionRequestsToPollOptions(request.getOptions());

        for (PollOption option : options) {
            option.setPoll(savedPoll);
        }

        List<PollOption> savedOptions = pollOptionRepository.saveAll(options);

        List<PollOptionResponse> optionResponses = pollMapper.pollOptionsToPollOptionResponses(savedOptions);

        return pollMapper.pollToPollResponse(savedPoll, optionResponses);
    }


    private void validateUserOwnsSession(Session session, User user) {
        if (!session.getOwner().getId().equals(user.getId())) {
            throw new AppException(
                    FORBIDDEN,
                    HttpStatus.FORBIDDEN,
                    "User does not own this session"
            );
        }
    }

    private Session getLiveOrScheduledSessionOrThrow(Long sessionId) {
        Session session = sessionService.getSessionById(sessionId);

        if (session == null) {
            throw new AppException(
                    RESOURCE_NOT_FOUND,
                    NOT_FOUND,
                    "Session not found"
            );
        }

        if (LIVE != session.getStatus() && SCHEDULED != session.getStatus()) {
            throw new AppException(
                    INVALID_SESSION_STATUS,
                    BAD_REQUEST,
                    "Only live or Scheduled sessions can receive polls"
            );
        }

        return session;
    }

    private void broadcastPollEventToPublicTopic(Poll poll, List<PollOptionResponse> options, PollEventType eventType) {
        PollEvent event = buildPollEvent(poll, options, eventType);

        messagingTemplate.convertAndSend(
                POLLS_TOPIC_TEMPLATE.formatted(event.getSessionId()),
                event
        );
    }

    private PollEvent buildPollEvent(Poll poll, List<PollOptionResponse> options, PollEventType eventType) {
        PollEvent event = pollMapper.pollToPollEvent(poll, options);

        event.setEventType(eventType);
        return event;
    }

}