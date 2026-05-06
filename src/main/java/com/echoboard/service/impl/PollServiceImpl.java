package com.echoboard.service.impl;

import com.echoboard.dto.poll.CreatePollRequest;
import com.echoboard.dto.poll.PollOptionResponse;
import com.echoboard.dto.poll.PollResponse;
import com.echoboard.dto.websocket.PollEvent;
import com.echoboard.entity.Participant;
import com.echoboard.entity.Poll;
import com.echoboard.entity.PollOption;
import com.echoboard.entity.PollVote;
import com.echoboard.entity.Session;
import com.echoboard.entity.User;
import com.echoboard.enums.PollEventType;
import com.echoboard.enums.PollStatus;
import com.echoboard.exception.AppException;
import com.echoboard.mapper.PollMapper;
import com.echoboard.repository.PollOptionRepository;
import com.echoboard.repository.PollRepository;
import com.echoboard.repository.PollVoteRepository;
import com.echoboard.service.CurrentParticipantService;
import com.echoboard.service.CurrentUserService;
import com.echoboard.service.ParticipantService;
import com.echoboard.service.PollService;
import com.echoboard.service.SessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static com.echoboard.enums.PollEventType.PUBLISHED;
import static com.echoboard.enums.PollEventType.UPDATED;
import static com.echoboard.enums.PollStatus.DRAFT;
import static com.echoboard.enums.SessionStatus.LIVE;
import static com.echoboard.enums.SessionStatus.SCHEDULED;
import static com.echoboard.exception.ErrorCode.FORBIDDEN;
import static com.echoboard.exception.ErrorCode.INVALID_POLL_STATUS;
import static com.echoboard.exception.ErrorCode.INVALID_SESSION_STATUS;
import static com.echoboard.exception.ErrorCode.RESOURCE_NOT_FOUND;
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
    private final ParticipantService participantService;

    @Override
    public PollResponse submitDraftPoll(CreatePollRequest request, Long sessionId) {
        User user = currentUserService.getCurrentUser();
        Session session = getLiveOrScheduledSessionOrThrow(sessionId);

        validateUserOwnsSession(session, user);

        Poll savedPoll = createAndSaveDraftPoll(request, session);
        List<PollOption> savedOptions = createAndSavePollOptions(request, savedPoll);

        return buildPollResponse(savedPoll, savedOptions);
    }

    @Override
    public PollResponse publishPoll(Long pollId, Long sessionId) {
        User user = currentUserService.getCurrentUser();
        Session session = getLiveSessionOrThrow(sessionId);

        validateUserOwnsSession(session, user);

        Poll poll = getPollOrThrow(pollId);
        validatePollBelongsToSession(poll, session);
        validatePollIsDraft(poll);

        poll.setStatus(PollStatus.PUBLISHED);
        poll.setPublishedAt(LocalDateTime.now());

        Poll savedPoll = pollRepository.save(poll);
        List<PollOption> pollOptions = getPollOptions(savedPoll.getId());

        PollResponse response = buildPollResponse(savedPoll, pollOptions);
        broadcastPollEventToPublicTopic(savedPoll, response.getOptions(), PUBLISHED);

        return response;
    }

    @Override
    public PollResponse voteOnPoll(Long pollId  , Long sessionId, Long pollOptionId) {
        Long participantId = currentParticipantService.getCurrentParticipantId();
        validateParticipantIdentity(participantId);

        Session session = getLiveSessionOrThrow(sessionId);

        Participant participant = getParticipantOrThrow(participantId);
        validateParticipantCanVoteInSession(participant, sessionId);

        Poll poll = getPollOrThrow(pollId);
        validatePollBelongsToSession(poll, session);
        validatePollIsPublished(poll);

        PollOption pollOption = getPollOptionOrThrow(pollOptionId);
        validatePollOptionBelongsToPoll(pollOption, poll);

        validateParticipantHasNotVotedBefore(participantId, pollId);

        savePollVote(participant, poll, pollOption);
        PollOption savedPollOption = incrementPollOptionVoteCount(pollOption);

        List<PollOption> pollOptions = getPollOptions(poll.getId());
        PollResponse response = buildPollResponse(poll, pollOptions);

        broadcastPollEventToPublicTopic(poll, response.getOptions(), UPDATED);

        return response;
    }

    private Poll createAndSaveDraftPoll(CreatePollRequest request, Session session) {
        Poll poll = pollMapper.createPollRequestToPoll(request);
        poll.setSession(session);

        return pollRepository.save(poll);
    }

    private List<PollOption> createAndSavePollOptions(CreatePollRequest request, Poll poll) {
        List<PollOption> options = pollMapper.pollOptionRequestsToPollOptions(request.getOptions());

        for (PollOption option : options) {
            option.setPoll(poll);
        }

        return pollOptionRepository.saveAll(options);
    }

    private void savePollVote(Participant participant, Poll poll, PollOption pollOption) {
        PollVote pollVote = PollVote
                .builder()
                .participant(participant)
                .poll(poll)
                .pollOption(pollOption)
                .build();

        pollVoteRepository.save(pollVote);
    }

    private PollOption incrementPollOptionVoteCount(PollOption pollOption) {
        pollOption.setVoteCount(pollOption.getVoteCount() + 1);

        return pollOptionRepository.save(pollOption);
    }

    private PollResponse buildPollResponse(Poll poll, List<PollOption> options) {
        List<PollOptionResponse> optionResponses = pollMapper.pollOptionsToPollOptionResponses(options);

        return pollMapper.pollToPollResponse(poll, optionResponses);
    }

    private List<PollOption> getPollOptions(Long pollId) {
        return pollOptionRepository.findByPoll_Id(pollId);
    }

    private Poll getPollOrThrow(Long pollId) {
        return pollRepository.findById(pollId)
                .orElseThrow(() -> new AppException(
                        RESOURCE_NOT_FOUND,
                        NOT_FOUND,
                        "Poll not found"
                ));
    }

    private PollOption getPollOptionOrThrow(Long pollOptionId) {
        return pollOptionRepository.findById(pollOptionId)
                .orElseThrow(() -> new AppException(
                        RESOURCE_NOT_FOUND,
                        NOT_FOUND,
                        "Poll option not found"
                ));
    }

    private Participant getParticipantOrThrow(Long participantId) {
        Participant participant = participantService.getParticipantById(participantId);

        if (participant == null) {
            throw new AppException(
                    RESOURCE_NOT_FOUND,
                    NOT_FOUND,
                    "Participant not found"
            );
        }

        return participant;
    }

    private void validatePollBelongsToSession(Poll poll, Session session) {
        if (!poll.getSession().getId().equals(session.getId())) {
            throw new AppException(
                    RESOURCE_NOT_FOUND,
                    NOT_FOUND,
                    "Poll not found"
            );
        }
    }

    private void validatePollOptionBelongsToPoll(PollOption pollOption, Poll poll) {
        if (!pollOption.getPoll().getId().equals(poll.getId())) {
            throw new AppException(
                    RESOURCE_NOT_FOUND,
                    NOT_FOUND,
                    "Poll option not found"
            );
        }
    }

    private void validatePollIsDraft(Poll poll) {
        if (!DRAFT.equals(poll.getStatus())) {
            throw new AppException(
                    INVALID_POLL_STATUS,
                    BAD_REQUEST,
                    "Only draft polls can be published"
            );
        }
    }

    private void validatePollIsPublished(Poll poll) {
        if (!PollStatus.PUBLISHED.equals(poll.getStatus())) {
            throw new AppException(
                    INVALID_POLL_STATUS,
                    BAD_REQUEST,
                    "You can vote only on published polls"
            );
        }
    }

    private void validateParticipantHasNotVotedBefore(Long participantId, Long pollId) {
        if (pollVoteRepository.existsByParticipant_IdAndPoll_Id(participantId, pollId)) {
            throw new AppException(
                    FORBIDDEN,
                    HttpStatus.FORBIDDEN,
                    "Participant cannot vote on the same poll again"
            );
        }
    }

    private void validateParticipantCanVoteInSession(Participant participant, Long sessionId) {
        validateParticipantBelongsToSession(participant, sessionId);
        validateParticipantIsNotMuted(participant);
    }

    private void validateParticipantIdentity(Long participantId) {
        if (participantId == null) {
            throw new AppException(
                    FORBIDDEN,
                    HttpStatus.FORBIDDEN,
                    "Only participants can vote on polls"
            );
        }
    }

    private void validateParticipantBelongsToSession(Participant participant, Long sessionId) {
        if (!participant.getSession().getId().equals(sessionId)) {
            throw new AppException(
                    FORBIDDEN,
                    HttpStatus.FORBIDDEN,
                    "Participant is not allowed to access this session"
            );
        }
    }

    private void validateParticipantIsNotMuted(Participant participant) {
        if (participant.getMutedUntil() != null && participant.getMutedUntil().isAfter(LocalDateTime.now())) {
            throw new AppException(
                    FORBIDDEN,
                    HttpStatus.FORBIDDEN,
                    "Cannot vote while muted"
            );
        }
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

    private Session getLiveSessionOrThrow(Long sessionId) {
        Session session = getSessionOrThrow(sessionId);

        if (LIVE != session.getStatus()) {
            throw new AppException(
                    INVALID_SESSION_STATUS,
                    BAD_REQUEST,
                    "Only live sessions can publish or vote on polls"
            );
        }

        return session;
    }

    private Session getLiveOrScheduledSessionOrThrow(Long sessionId) {
        Session session = getSessionOrThrow(sessionId);

        if (LIVE != session.getStatus() && SCHEDULED != session.getStatus()) {
            throw new AppException(
                    INVALID_SESSION_STATUS,
                    BAD_REQUEST,
                    "Only live or scheduled sessions can receive polls"
            );
        }

        return session;
    }

    private Session getSessionOrThrow(Long sessionId) {
        Session session = sessionService.getSessionById(sessionId);

        if (session == null) {
            throw new AppException(
                    RESOURCE_NOT_FOUND,
                    NOT_FOUND,
                    "Session not found"
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