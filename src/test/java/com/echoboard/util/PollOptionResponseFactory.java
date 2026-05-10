package com.echoboard.util;

import com.echoboard.dto.poll.PollOptionResponse;

public final class PollOptionResponseFactory {

    private PollOptionResponseFactory() {
    }

    public static PollOptionResponse optionResponse() {
        return new PollOptionResponse(
                1L,
                "Spring Boot",
                5
        );
    }

    public static PollOptionResponse optionResponse(Long id, String text, int voteCount) {
        return new PollOptionResponse(
                id,
                text,
                voteCount
        );
    }
}