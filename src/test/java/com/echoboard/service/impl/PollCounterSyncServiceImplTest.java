package com.echoboard.service.impl;

import com.echoboard.entity.Poll;
import com.echoboard.entity.PollOption;
import com.echoboard.repository.PollOptionRepository;
import com.echoboard.repository.PollRepository;
import com.echoboard.service.PollOptionCountCacheService;
import com.echoboard.util.PollEntityFactory;
import com.echoboard.util.PollOptionEntityFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PollCounterSyncServiceImplTest {

    @Mock
    private PollRepository pollRepository;

    @Mock
    private PollOptionRepository pollOptionRepository;

    @Mock
    private PollOptionCountCacheService pollOptionCountCacheService;

    @InjectMocks
    private PollCounterSyncServiceImpl pollCounterSyncService;

    @Test
    void syncPollCountersForSession_shouldSyncCountsForEveryPollInSession() {
        Long sessionId = 1L;

        Poll firstPoll = PollEntityFactory.publishedPoll();
        firstPoll.setId(10L);

        Poll secondPoll = PollEntityFactory.publishedPoll();
        secondPoll.setId(20L);

        when(pollRepository.findBySession_IdOrderByCreatedAtAsc(sessionId))
                .thenReturn(List.of(firstPoll, secondPoll));

        when(pollOptionRepository.findByPoll_IdOrderByIdAsc(10L)).thenReturn(List.of());
        when(pollOptionRepository.findByPoll_IdOrderByIdAsc(20L)).thenReturn(List.of());

        pollCounterSyncService.syncPollCountersForSession(sessionId);

        verify(pollRepository).findBySession_IdOrderByCreatedAtAsc(sessionId);
        verify(pollOptionRepository).findByPoll_IdOrderByIdAsc(10L);
        verify(pollOptionRepository).findByPoll_IdOrderByIdAsc(20L);

        verify(pollOptionRepository, never()).saveAll(List.of());
    }

    @Test
    void syncPollCountersForSession_whenSessionHasNoPolls_shouldDoNothing() {
        Long sessionId = 1L;

        when(pollRepository.findBySession_IdOrderByCreatedAtAsc(sessionId))
                .thenReturn(List.of());

        pollCounterSyncService.syncPollCountersForSession(sessionId);

        verify(pollRepository).findBySession_IdOrderByCreatedAtAsc(sessionId);
        verify(pollOptionRepository, never()).findByPoll_IdOrderByIdAsc(1L);
        verify(pollOptionRepository, never()).saveAll(List.of());
    }

    @Test
    void syncPollCounts_whenRedisCountIsNull_shouldNotUpdateOrSaveOption() {
        Long pollId = 1L;

        Poll poll = PollEntityFactory.publishedPoll();
        poll.setId(pollId);

        PollOption option = PollOptionEntityFactory.optionForPoll(poll);
        option.setId(10L);
        option.setVoteCount(5);

        when(pollOptionRepository.findByPoll_IdOrderByIdAsc(pollId))
                .thenReturn(List.of(option));

        when(pollOptionCountCacheService.getCachedPollOptionCount(pollId, option.getId()))
                .thenReturn(null);

        pollCounterSyncService.syncPollCounts(pollId);

        assertEquals(5, option.getVoteCount());

        verify(pollOptionRepository).findByPoll_IdOrderByIdAsc(pollId);
        verify(pollOptionCountCacheService).getCachedPollOptionCount(pollId, option.getId());
        verify(pollOptionRepository, never()).saveAll(List.of(option));
    }

    @Test
    void syncPollCounts_whenRedisCountEqualsDbCount_shouldNotSaveOption() {
        Long pollId = 1L;

        Poll poll = PollEntityFactory.publishedPoll();
        poll.setId(pollId);

        PollOption option = PollOptionEntityFactory.optionForPoll(poll);
        option.setId(10L);
        option.setVoteCount(5);

        when(pollOptionRepository.findByPoll_IdOrderByIdAsc(pollId))
                .thenReturn(List.of(option));

        when(pollOptionCountCacheService.getCachedPollOptionCount(pollId, option.getId()))
                .thenReturn(5);

        pollCounterSyncService.syncPollCounts(pollId);

        assertEquals(5, option.getVoteCount());

        verify(pollOptionRepository).findByPoll_IdOrderByIdAsc(pollId);
        verify(pollOptionCountCacheService).getCachedPollOptionCount(pollId, option.getId());
        verify(pollOptionRepository, never()).saveAll(List.of(option));
    }

    @Test
    void syncPollCounts_whenRedisCountDiffersFromDbCount_shouldUpdateAndSaveOption() {
        Long pollId = 1L;

        Poll poll = PollEntityFactory.publishedPoll();
        poll.setId(pollId);

        PollOption option = PollOptionEntityFactory.optionForPoll(poll);
        option.setId(10L);
        option.setVoteCount(5);

        when(pollOptionRepository.findByPoll_IdOrderByIdAsc(pollId))
                .thenReturn(List.of(option));

        when(pollOptionCountCacheService.getCachedPollOptionCount(pollId, option.getId()))
                .thenReturn(9);

        pollCounterSyncService.syncPollCounts(pollId);

        ArgumentCaptor<List<PollOption>> optionsCaptor = ArgumentCaptor.forClass(List.class);

        verify(pollOptionRepository).saveAll(optionsCaptor.capture());

        List<PollOption> savedOptions = optionsCaptor.getValue();

        assertEquals(1, savedOptions.size());
        assertEquals(option, savedOptions.get(0));
        assertEquals(9, option.getVoteCount());

        verify(pollOptionRepository).findByPoll_IdOrderByIdAsc(pollId);
        verify(pollOptionCountCacheService).getCachedPollOptionCount(pollId, option.getId());
    }

    @Test
    void syncPollCounts_whenMultipleOptionsHaveDifferentRedisCounts_shouldSaveOnlyUpdatedOptions() {
        Long pollId = 1L;

        Poll poll = PollEntityFactory.publishedPoll();
        poll.setId(pollId);

        PollOption firstOption = PollOptionEntityFactory.optionForPoll(
                10L,
                poll,
                "Spring Boot",
                5
        );

        PollOption secondOption = PollOptionEntityFactory.optionForPoll(
                20L,
                poll,
                "Redis",
                3
        );

        PollOption thirdOption = PollOptionEntityFactory.optionForPoll(
                30L,
                poll,
                "RabbitMQ",
                7
        );

        when(pollOptionRepository.findByPoll_IdOrderByIdAsc(pollId))
                .thenReturn(List.of(firstOption, secondOption, thirdOption));

        when(pollOptionCountCacheService.getCachedPollOptionCount(pollId, 10L))
                .thenReturn(5);

        when(pollOptionCountCacheService.getCachedPollOptionCount(pollId, 20L))
                .thenReturn(8);

        when(pollOptionCountCacheService.getCachedPollOptionCount(pollId, 30L))
                .thenReturn(null);

        pollCounterSyncService.syncPollCounts(pollId);

        ArgumentCaptor<List<PollOption>> optionsCaptor = ArgumentCaptor.forClass(List.class);

        verify(pollOptionRepository).saveAll(optionsCaptor.capture());

        List<PollOption> savedOptions = optionsCaptor.getValue();

        assertEquals(1, savedOptions.size());
        assertEquals(secondOption, savedOptions.get(0));

        assertEquals(5, firstOption.getVoteCount());
        assertEquals(8, secondOption.getVoteCount());
        assertEquals(7, thirdOption.getVoteCount());

        verify(pollOptionRepository).findByPoll_IdOrderByIdAsc(pollId);
        verify(pollOptionCountCacheService).getCachedPollOptionCount(pollId, 10L);
        verify(pollOptionCountCacheService).getCachedPollOptionCount(pollId, 20L);
        verify(pollOptionCountCacheService).getCachedPollOptionCount(pollId, 30L);
    }
}