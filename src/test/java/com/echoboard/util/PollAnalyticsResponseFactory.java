package com.echoboard.util;

import com.echoboard.dto.analytics.PollAnalyticsResponse;
import com.echoboard.dto.analytics.PollOptionAnalyticsResponse;
import com.echoboard.enums.PollStatus;
import com.echoboard.enums.PollType;

import java.time.LocalDateTime;
import java.util.List;

public final class PollAnalyticsResponseFactory {

    private PollAnalyticsResponseFactory() {
    }

    public static PollAnalyticsResponse pollAnalyticsResponse() {
        PollOptionAnalyticsResponse firstOption = PollOptionAnalyticsResponse
                .builder()
                .optionId(1L)
                .text("Spring Boot")
                .voteCount(7)
                .percentage(70.0)
                .build();

        PollOptionAnalyticsResponse secondOption = PollOptionAnalyticsResponse
                .builder()
                .optionId(2L)
                .text("Redis")
                .voteCount(3)
                .percentage(30.0)
                .build();

        return PollAnalyticsResponse
                .builder()
                .pollId(1L)
                .title("Best backend topic?")
                .status(PollStatus.CLOSED)
                .type(PollType.SINGLE_CHOICE)
                .totalVotes(10)
                .publishedAt(LocalDateTime.now().minusMinutes(30))
                .closedAt(LocalDateTime.now().minusMinutes(5))
                .options(List.of(firstOption, secondOption))
                .build();
    }
}