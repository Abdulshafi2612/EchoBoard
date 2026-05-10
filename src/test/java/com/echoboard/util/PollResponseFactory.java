package com.echoboard.util;

import com.echoboard.dto.poll.PollOptionResponse;
import com.echoboard.dto.poll.PollResponse;
import com.echoboard.entity.Poll;

import java.util.List;

public final class PollResponseFactory {

    private PollResponseFactory() {
    }

    public static PollResponse fromPoll(Poll poll, List<PollOptionResponse> options) {
        return new PollResponse(
                poll.getId(),
                poll.getSession().getId(),
                poll.getTitle(),
                poll.getStatus(),
                poll.getType(),
                options,
                poll.getCreatedAt(),
                poll.getPublishedAt(),
                poll.getClosedAt()
        );
    }
}