package com.echoboard.service;

import com.echoboard.dto.poll.CreatePollRequest;
import com.echoboard.dto.poll.PollResponse;

public interface PollService {

    PollResponse submitDraftPoll(CreatePollRequest request, Long sessionId);

    PollResponse publishPoll(Long pollId, Long sessionId);

    PollResponse closePoll(Long pollId, Long sessionId);

    PollResponse voteOnPoll(Long pollId, Long sessionId, Long optionId);

    void deletePoll(Long pollId, Long sessionId);

    long getNumberOfTotalPollsBySessionId(Long sessionId);

    long getNumberOfTotalVotesBySessionId(Long sessionId);

}
