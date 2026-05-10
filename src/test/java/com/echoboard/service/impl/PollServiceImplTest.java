package com.echoboard.service.impl;

import com.echoboard.dto.analytics.PollAnalyticsResponse;
import com.echoboard.dto.analytics.PollOptionAnalyticsResponse;
import com.echoboard.dto.poll.CreatePollRequest;
import com.echoboard.dto.poll.PollOptionRequest;
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
import com.echoboard.enums.PollType;
import com.echoboard.enums.SessionStatus;
import com.echoboard.exception.AppException;
import com.echoboard.mapper.PollMapper;
import com.echoboard.repository.PollOptionRepository;
import com.echoboard.repository.PollRepository;
import com.echoboard.repository.PollVoteRepository;
import com.echoboard.service.CurrentParticipantService;
import com.echoboard.service.CurrentUserService;
import com.echoboard.service.ParticipantService;
import com.echoboard.service.PollOptionCountCacheService;
import com.echoboard.service.SessionService;
import com.echoboard.util.ParticipantEntityFactory;
import com.echoboard.util.PollEntityFactory;
import com.echoboard.util.PollOptionEntityFactory;
import com.echoboard.util.PollOptionResponseFactory;
import com.echoboard.util.PollResponseFactory;
import com.echoboard.util.SessionEntityFactory;
import com.echoboard.util.UserEntityFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;
import java.util.Optional;

import static com.echoboard.exception.ErrorCode.FORBIDDEN;
import static com.echoboard.exception.ErrorCode.INVALID_POLL_STATUS;
import static com.echoboard.exception.ErrorCode.INVALID_SESSION_STATUS;
import static com.echoboard.exception.ErrorCode.RESOURCE_NOT_FOUND;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PollServiceImplTest {

    @Mock
    private PollRepository pollRepository;

    @Mock
    private PollOptionRepository pollOptionRepository;

    @Mock
    private PollVoteRepository pollVoteRepository;

    @Mock
    private PollMapper pollMapper;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private CurrentParticipantService currentParticipantService;

    @Mock
    private SessionService sessionService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private ParticipantService participantService;

    @Mock
    private PollOptionCountCacheService pollOptionCountCacheService;

    @InjectMocks
    private PollServiceImpl pollService;

    @Test
    void submitDraftPoll_whenSessionIsLiveAndUserOwnsSession_shouldCreateDraftPollOptionsAndReturnResponse() {
        Long sessionId = 1L;

        User user = UserEntityFactory.presenter();
        Session session = SessionEntityFactory.liveSessionOwnedBy(user);
        session.setId(sessionId);

        CreatePollRequest request = CreatePollRequest
                .builder()
                .title("Best backend topic?")
                .type(PollType.SINGLE_CHOICE)
                .options(List.of(
                        PollOptionRequest.builder().text("Spring Boot").build(),
                        PollOptionRequest.builder().text("Redis").build()
                ))
                .build();

        Poll mappedPoll = new Poll();
        mappedPoll.setTitle(request.getTitle());
        mappedPoll.setType(request.getType());

        Poll savedPoll = PollEntityFactory.draftPoll();
        savedPoll.setId(10L);
        savedPoll.setSession(session);
        savedPoll.setTitle(request.getTitle());
        savedPoll.setType(request.getType());

        PollOption firstOption = PollOptionEntityFactory.optionForPoll(1L, savedPoll, "Spring Boot", 0);
        PollOption secondOption = PollOptionEntityFactory.optionForPoll(2L, savedPoll, "Redis", 0);
        List<PollOption> mappedOptions = List.of(firstOption, secondOption);

        PollOptionResponse firstOptionResponse = PollOptionResponseFactory.optionResponse(1L, "Spring Boot", 0);
        PollOptionResponse secondOptionResponse = PollOptionResponseFactory.optionResponse(2L, "Redis", 0);
        List<PollOptionResponse> optionResponses = List.of(firstOptionResponse, secondOptionResponse);

        PollResponse expectedResponse = PollResponseFactory.fromPoll(savedPoll, optionResponses);

        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(sessionService.getSessionById(sessionId)).thenReturn(session);
        when(pollMapper.createPollRequestToPoll(request)).thenReturn(mappedPoll);
        when(pollRepository.save(mappedPoll)).thenReturn(savedPoll);
        when(pollMapper.pollOptionRequestsToPollOptions(request.getOptions())).thenReturn(mappedOptions);
        when(pollOptionRepository.saveAll(mappedOptions)).thenReturn(mappedOptions);
        when(pollMapper.pollOptionsToPollOptionResponses(mappedOptions)).thenReturn(optionResponses);
        when(pollOptionCountCacheService.getPollOptionCount(10L, 1L, 0)).thenReturn(0);
        when(pollOptionCountCacheService.getPollOptionCount(10L, 2L, 0)).thenReturn(0);
        when(pollMapper.pollToPollResponse(savedPoll, optionResponses)).thenReturn(expectedResponse);

        PollResponse response = pollService.submitDraftPoll(request, sessionId);

        assertEquals(expectedResponse, response);
        assertEquals(session, mappedPoll.getSession());
        assertEquals(savedPoll, firstOption.getPoll());
        assertEquals(savedPoll, secondOption.getPoll());

        verify(currentUserService).getCurrentUser();
        verify(sessionService).getSessionById(sessionId);
        verify(pollRepository).save(mappedPoll);
        verify(pollOptionRepository).saveAll(mappedOptions);
        verify(pollMapper).pollToPollResponse(savedPoll, optionResponses);
    }

    @Test
    void submitDraftPoll_whenSessionIsNotLiveOrScheduled_shouldThrowInvalidSessionStatusException() {
        Long sessionId = 1L;

        User user = UserEntityFactory.presenter();
        Session session = SessionEntityFactory.sessionWithStatus(SessionStatus.ENDED);
        session.setOwner(user);

        CreatePollRequest request = CreatePollRequest
                .builder()
                .title("Best backend topic?")
                .type(PollType.SINGLE_CHOICE)
                .options(List.of(
                        PollOptionRequest.builder().text("Spring Boot").build(),
                        PollOptionRequest.builder().text("Redis").build()
                ))
                .build();

        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(sessionService.getSessionById(sessionId)).thenReturn(session);

        AppException exception = assertThrows(
                AppException.class,
                () -> pollService.submitDraftPoll(request, sessionId)
        );

        assertEquals(INVALID_SESSION_STATUS, exception.getErrorCode());
        assertEquals("Only live or scheduled sessions can receive polls", exception.getMessage());

        verify(pollRepository, never()).save(any(Poll.class));
        verifyNoInteractions(pollOptionRepository);
    }

    @Test
    void publishPoll_whenDraftPollBelongsToLiveOwnedSession_shouldPublishPollBroadcastEventAndReturnResponse() {
        Long sessionId = 1L;
        Long pollId = 10L;

        User user = UserEntityFactory.presenter();
        Session session = SessionEntityFactory.liveSessionOwnedBy(user);
        session.setId(sessionId);

        Poll poll = PollEntityFactory.draftPoll();
        poll.setId(pollId);
        poll.setSession(session);

        Poll savedPoll = poll;

        PollOption option = PollOptionEntityFactory.optionForPoll(1L, poll, "Spring Boot", 5);
        List<PollOption> options = List.of(option);

        PollOptionResponse optionResponse = PollOptionResponseFactory.optionResponse(1L, "Spring Boot", 5);
        List<PollOptionResponse> optionResponses = List.of(optionResponse);

        PollResponse expectedResponse = PollResponseFactory.fromPoll(savedPoll, optionResponses);

        PollEvent event = PollEvent
                .builder()
                .id(pollId)
                .sessionId(sessionId)
                .title(poll.getTitle())
                .status(PollStatus.PUBLISHED)
                .type(poll.getType())
                .options(optionResponses)
                .build();

        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(sessionService.getSessionById(sessionId)).thenReturn(session);
        when(pollRepository.findById(pollId)).thenReturn(Optional.of(poll));
        when(pollRepository.save(poll)).thenReturn(savedPoll);
        when(pollOptionRepository.findByPoll_IdOrderByIdAsc(pollId)).thenReturn(options);
        when(pollMapper.pollOptionsToPollOptionResponses(options)).thenReturn(optionResponses);
        when(pollOptionCountCacheService.getPollOptionCount(pollId, 1L, 5)).thenReturn(5);
        when(pollMapper.pollToPollResponse(savedPoll, optionResponses)).thenReturn(expectedResponse);
        when(pollMapper.pollToPollEvent(savedPoll, optionResponses)).thenReturn(event);

        PollResponse response = pollService.publishPoll(pollId, sessionId);

        assertEquals(expectedResponse, response);
        assertEquals(PollStatus.PUBLISHED, poll.getStatus());
        assertNotNull(poll.getPublishedAt());

        ArgumentCaptor<PollEvent> eventCaptor = ArgumentCaptor.forClass(PollEvent.class);

        verify(pollRepository).save(poll);
        verify(messagingTemplate).convertAndSend(
                eq("/topic/sessions/1/polls"),
                eventCaptor.capture()
        );

        assertEquals(PollEventType.PUBLISHED, eventCaptor.getValue().getEventType());
    }

    @Test
    void publishPoll_whenPollIsNotDraft_shouldThrowInvalidPollStatusException() {
        Long sessionId = 1L;
        Long pollId = 10L;

        User user = UserEntityFactory.presenter();
        Session session = SessionEntityFactory.liveSessionOwnedBy(user);
        session.setId(sessionId);

        Poll poll = PollEntityFactory.publishedPoll();
        poll.setId(pollId);
        poll.setSession(session);

        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(sessionService.getSessionById(sessionId)).thenReturn(session);
        when(pollRepository.findById(pollId)).thenReturn(Optional.of(poll));

        AppException exception = assertThrows(
                AppException.class,
                () -> pollService.publishPoll(pollId, sessionId)
        );

        assertEquals(INVALID_POLL_STATUS, exception.getErrorCode());
        assertEquals("Poll must be DRAFT", exception.getMessage());

        verify(pollRepository, never()).save(any(Poll.class));
        verify(messagingTemplate, never()).convertAndSend(any(String.class), any(Object.class));
    }

    @Test
    void closePoll_whenPublishedPollBelongsToLiveOwnedSession_shouldClosePollSyncCountsBroadcastAndReturnResponse() {
        Long sessionId = 1L;
        Long pollId = 10L;

        User user = UserEntityFactory.presenter();
        Session session = SessionEntityFactory.liveSessionOwnedBy(user);
        session.setId(sessionId);

        Poll poll = PollEntityFactory.publishedPoll();
        poll.setId(pollId);
        poll.setSession(session);

        PollOption option = PollOptionEntityFactory.optionForPoll(1L, poll, "Spring Boot", 5);
        List<PollOption> options = List.of(option);

        PollOptionResponse optionResponse = PollOptionResponseFactory.optionResponse(1L, "Spring Boot", 9);
        List<PollOptionResponse> optionResponses = List.of(optionResponse);

        PollResponse expectedResponse = PollResponseFactory.fromPoll(poll, optionResponses);

        PollEvent event = PollEvent
                .builder()
                .id(pollId)
                .sessionId(sessionId)
                .options(optionResponses)
                .build();

        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(sessionService.getSessionById(sessionId)).thenReturn(session);
        when(pollRepository.findById(pollId)).thenReturn(Optional.of(poll));
        when(pollOptionRepository.findByPoll_IdOrderByIdAsc(pollId)).thenReturn(options);
        when(pollOptionCountCacheService.getCachedPollOptionCount(pollId, 1L)).thenReturn(9);
        when(pollOptionRepository.saveAll(List.of(option))).thenReturn(List.of(option));
        when(pollRepository.save(poll)).thenReturn(poll);
        when(pollMapper.pollOptionsToPollOptionResponses(options)).thenReturn(optionResponses);
        when(pollOptionCountCacheService.getPollOptionCount(pollId, 1L, 9)).thenReturn(9);
        when(pollMapper.pollToPollResponse(poll, optionResponses)).thenReturn(expectedResponse);
        when(pollMapper.pollToPollEvent(poll, optionResponses)).thenReturn(event);

        PollResponse response = pollService.closePoll(pollId, sessionId);

        assertEquals(expectedResponse, response);
        assertEquals(PollStatus.CLOSED, poll.getStatus());
        assertNotNull(poll.getClosedAt());
        assertEquals(9, option.getVoteCount());

        ArgumentCaptor<PollEvent> eventCaptor = ArgumentCaptor.forClass(PollEvent.class);

        verify(pollOptionRepository).saveAll(List.of(option));
        verify(pollRepository).save(poll);
        verify(messagingTemplate).convertAndSend(
                eq("/topic/sessions/1/polls"),
                eventCaptor.capture()
        );

        assertEquals(PollEventType.CLOSED, eventCaptor.getValue().getEventType());
    }

    @Test
    void voteOnPoll_whenParticipantCanVote_shouldSaveVoteIncrementCacheBroadcastAndReturnResponse() {
        Long sessionId = 1L;
        Long pollId = 10L;
        Long optionId = 100L;
        Long participantId = 50L;

        Session session = SessionEntityFactory.liveSession();
        session.setId(sessionId);

        Participant participant = ParticipantEntityFactory.participantForSession(session);
        participant.setId(participantId);

        Poll poll = PollEntityFactory.publishedPoll();
        poll.setId(pollId);
        poll.setSession(session);

        PollOption option = PollOptionEntityFactory.optionForPoll(optionId, poll, "Spring Boot", 5);
        List<PollOption> options = List.of(option);

        PollOptionResponse optionResponse = PollOptionResponseFactory.optionResponse(optionId, "Spring Boot", 6);
        List<PollOptionResponse> optionResponses = List.of(optionResponse);

        PollResponse expectedResponse = PollResponseFactory.fromPoll(poll, optionResponses);

        PollEvent event = PollEvent
                .builder()
                .id(pollId)
                .sessionId(sessionId)
                .options(optionResponses)
                .build();

        when(currentParticipantService.getCurrentParticipantId()).thenReturn(participantId);
        when(sessionService.getSessionById(sessionId)).thenReturn(session);
        when(participantService.getParticipantById(participantId)).thenReturn(participant);
        when(pollRepository.findById(pollId)).thenReturn(Optional.of(poll));
        when(pollOptionRepository.findById(optionId)).thenReturn(Optional.of(option));
        when(pollVoteRepository.existsByParticipant_IdAndPoll_Id(participantId, pollId)).thenReturn(false);
        when(pollOptionRepository.findByPoll_IdOrderByIdAsc(pollId)).thenReturn(options);
        when(pollMapper.pollOptionsToPollOptionResponses(options)).thenReturn(optionResponses);
        when(pollOptionCountCacheService.getPollOptionCount(pollId, optionId, 6)).thenReturn(6);
        when(pollMapper.pollToPollResponse(poll, optionResponses)).thenReturn(expectedResponse);
        when(pollMapper.pollToPollEvent(poll, optionResponses)).thenReturn(event);

        PollResponse response = pollService.voteOnPoll(pollId, sessionId, optionId);

        assertEquals(expectedResponse, response);

        ArgumentCaptor<PollVote> pollVoteCaptor = ArgumentCaptor.forClass(PollVote.class);
        ArgumentCaptor<PollEvent> eventCaptor = ArgumentCaptor.forClass(PollEvent.class);

        verify(pollVoteRepository).save(pollVoteCaptor.capture());
        verify(pollOptionCountCacheService).incrementPollOptionCount(pollId, optionId, 5);
        verify(messagingTemplate).convertAndSend(
                eq("/topic/sessions/1/polls"),
                eventCaptor.capture()
        );

        PollVote savedVote = pollVoteCaptor.getValue();

        assertEquals(participant, savedVote.getParticipant());
        assertEquals(poll, savedVote.getPoll());
        assertEquals(option, savedVote.getPollOption());
        assertEquals(PollEventType.UPDATED, eventCaptor.getValue().getEventType());
    }

    @Test
    void voteOnPoll_whenCurrentParticipantIdIsNull_shouldThrowForbiddenException() {
        when(currentParticipantService.getCurrentParticipantId()).thenReturn(null);

        AppException exception = assertThrows(
                AppException.class,
                () -> pollService.voteOnPoll(10L, 1L, 100L)
        );

        assertEquals(FORBIDDEN, exception.getErrorCode());
        assertEquals("Only participants can vote on polls", exception.getMessage());

        verifyNoInteractions(sessionService);
        verifyNoInteractions(participantService);
        verifyNoInteractions(pollRepository);
    }

    @Test
    void voteOnPoll_whenParticipantAlreadyVoted_shouldThrowForbiddenException() {
        Long sessionId = 1L;
        Long pollId = 10L;
        Long optionId = 100L;
        Long participantId = 50L;

        Session session = SessionEntityFactory.liveSession();
        session.setId(sessionId);

        Participant participant = ParticipantEntityFactory.participantForSession(session);
        participant.setId(participantId);

        Poll poll = PollEntityFactory.publishedPoll();
        poll.setId(pollId);
        poll.setSession(session);

        PollOption option = PollOptionEntityFactory.optionForPoll(optionId, poll, "Spring Boot", 5);

        when(currentParticipantService.getCurrentParticipantId()).thenReturn(participantId);
        when(sessionService.getSessionById(sessionId)).thenReturn(session);
        when(participantService.getParticipantById(participantId)).thenReturn(participant);
        when(pollRepository.findById(pollId)).thenReturn(Optional.of(poll));
        when(pollOptionRepository.findById(optionId)).thenReturn(Optional.of(option));
        when(pollVoteRepository.existsByParticipant_IdAndPoll_Id(participantId, pollId)).thenReturn(true);

        AppException exception = assertThrows(
                AppException.class,
                () -> pollService.voteOnPoll(pollId, sessionId, optionId)
        );

        assertEquals(FORBIDDEN, exception.getErrorCode());
        assertEquals("Participant cannot vote on the same poll again", exception.getMessage());

        verify(pollVoteRepository, never()).save(any(PollVote.class));
        verify(pollOptionCountCacheService, never()).incrementPollOptionCount(any(), any(), any(Integer.class));
    }

    @Test
    void deletePoll_whenDraftPollBelongsToLiveOwnedSession_shouldDeleteOptionsAndPoll() {
        Long sessionId = 1L;
        Long pollId = 10L;

        User user = UserEntityFactory.presenter();
        Session session = SessionEntityFactory.liveSessionOwnedBy(user);
        session.setId(sessionId);

        Poll poll = PollEntityFactory.draftPoll();
        poll.setId(pollId);
        poll.setSession(session);

        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(sessionService.getSessionById(sessionId)).thenReturn(session);
        when(pollRepository.findById(pollId)).thenReturn(Optional.of(poll));

        pollService.deletePoll(pollId, sessionId);

        verify(pollOptionRepository).deleteByPoll_Id(pollId);
        verify(pollRepository).delete(poll);
    }

    @Test
    void deletePoll_whenPollIsNotDraft_shouldThrowInvalidPollStatusException() {
        Long sessionId = 1L;
        Long pollId = 10L;

        User user = UserEntityFactory.presenter();
        Session session = SessionEntityFactory.liveSessionOwnedBy(user);
        session.setId(sessionId);

        Poll poll = PollEntityFactory.publishedPoll();
        poll.setId(pollId);
        poll.setSession(session);

        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(sessionService.getSessionById(sessionId)).thenReturn(session);
        when(pollRepository.findById(pollId)).thenReturn(Optional.of(poll));

        AppException exception = assertThrows(
                AppException.class,
                () -> pollService.deletePoll(pollId, sessionId)
        );

        assertEquals(INVALID_POLL_STATUS, exception.getErrorCode());
        assertEquals("Poll must be DRAFT", exception.getMessage());

        verify(pollOptionRepository, never()).deleteByPoll_Id(pollId);
        verify(pollRepository, never()).delete(any(Poll.class));
    }

    @Test
    void getNumberOfTotalPollsBySessionId_shouldReturnRepositoryCount() {
        when(pollRepository.countBySession_Id(1L)).thenReturn(3L);

        long count = pollService.getNumberOfTotalPollsBySessionId(1L);

        assertEquals(3L, count);

        verify(pollRepository).countBySession_Id(1L);
    }

    @Test
    void getNumberOfTotalVotesBySessionId_shouldReturnRepositoryCount() {
        when(pollVoteRepository.countByPoll_Session_Id(1L)).thenReturn(12L);

        long count = pollService.getNumberOfTotalVotesBySessionId(1L);

        assertEquals(12L, count);

        verify(pollVoteRepository).countByPoll_Session_Id(1L);
    }

    @Test
    void getPollAnalyticsBySessionId_shouldReturnMappedPollAnalytics() {
        Long sessionId = 1L;

        Poll poll = PollEntityFactory.closedPoll();
        poll.setId(10L);

        PollOption firstOption = PollOptionEntityFactory.optionForPoll(1L, poll, "Spring Boot", 7);
        PollOption secondOption = PollOptionEntityFactory.optionForPoll(2L, poll, "Redis", 3);

        PollOptionAnalyticsResponse firstOptionResponse = PollOptionAnalyticsResponse
                .builder()
                .optionId(1L)
                .text("Spring Boot")
                .voteCount(7)
                .percentage(70.0)
                .build();

        PollOptionAnalyticsResponse secondOptionResponse = PollOptionAnalyticsResponse
                .builder()
                .optionId(2L)
                .text("Redis")
                .voteCount(3)
                .percentage(30.0)
                .build();

        PollAnalyticsResponse expectedResponse = PollAnalyticsResponse
                .builder()
                .pollId(10L)
                .title(poll.getTitle())
                .status(poll.getStatus())
                .type(poll.getType())
                .totalVotes(10)
                .options(List.of(firstOptionResponse, secondOptionResponse))
                .build();

        when(pollRepository.findBySession_IdOrderByCreatedAtAsc(sessionId)).thenReturn(List.of(poll));
        when(pollOptionRepository.findByPoll_IdOrderByIdAsc(10L)).thenReturn(List.of(firstOption, secondOption));
        when(pollMapper.pollOptionToPollOptionAnalyticsResponse(firstOption, 7L, 70.0)).thenReturn(firstOptionResponse);
        when(pollMapper.pollOptionToPollOptionAnalyticsResponse(secondOption, 3L, 30.0)).thenReturn(secondOptionResponse);
        when(pollMapper.pollToPollAnalyticsResponse(
                poll,
                10L,
                List.of(firstOptionResponse, secondOptionResponse)
        )).thenReturn(expectedResponse);

        List<PollAnalyticsResponse> responses = pollService.getPollAnalyticsBySessionId(sessionId);

        assertEquals(1, responses.size());
        assertEquals(expectedResponse, responses.get(0));
    }

    @Test
    void getPollAnalyticsBySessionId_whenPollHasZeroVotes_shouldUseZeroPercentage() {
        Long sessionId = 1L;

        Poll poll = PollEntityFactory.publishedPoll();
        poll.setId(10L);

        PollOption option = PollOptionEntityFactory.optionForPoll(1L, poll, "Spring Boot", 0);

        PollOptionAnalyticsResponse optionResponse = PollOptionAnalyticsResponse
                .builder()
                .optionId(1L)
                .text("Spring Boot")
                .voteCount(0)
                .percentage(0.0)
                .build();

        PollAnalyticsResponse expectedResponse = PollAnalyticsResponse
                .builder()
                .pollId(10L)
                .totalVotes(0)
                .options(List.of(optionResponse))
                .build();

        when(pollRepository.findBySession_IdOrderByCreatedAtAsc(sessionId)).thenReturn(List.of(poll));
        when(pollOptionRepository.findByPoll_IdOrderByIdAsc(10L)).thenReturn(List.of(option));
        when(pollMapper.pollOptionToPollOptionAnalyticsResponse(option, 0L, 0.0)).thenReturn(optionResponse);
        when(pollMapper.pollToPollAnalyticsResponse(poll, 0L, List.of(optionResponse))).thenReturn(expectedResponse);

        List<PollAnalyticsResponse> responses = pollService.getPollAnalyticsBySessionId(sessionId);

        assertEquals(1, responses.size());
        assertEquals(expectedResponse, responses.get(0));
    }

    @Test
    void publishPoll_whenPollDoesNotExist_shouldThrowResourceNotFoundException() {
        Long sessionId = 1L;
        Long pollId = 404L;

        User user = UserEntityFactory.presenter();
        Session session = SessionEntityFactory.liveSessionOwnedBy(user);

        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(sessionService.getSessionById(sessionId)).thenReturn(session);
        when(pollRepository.findById(pollId)).thenReturn(Optional.empty());

        AppException exception = assertThrows(
                AppException.class,
                () -> pollService.publishPoll(pollId, sessionId)
        );

        assertEquals(RESOURCE_NOT_FOUND, exception.getErrorCode());
        assertEquals("Poll not found", exception.getMessage());
    }
}