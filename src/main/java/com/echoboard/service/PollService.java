package com.echoboard.service;

import com.echoboard.dto.poll.CreatePollRequest;
import com.echoboard.dto.poll.PollResponse;

public interface PollService {

    PollResponse submitDraftPoll(CreatePollRequest request, Long sessionId);

    PollResponse publishPoll(Long pollId, Long sessionId);
}
