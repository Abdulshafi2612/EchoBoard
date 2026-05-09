package com.echoboard.dto.analytics;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PollOptionAnalyticsResponse {

    private Long optionId;

    private String text;

    private long voteCount;

    private double percentage;
}