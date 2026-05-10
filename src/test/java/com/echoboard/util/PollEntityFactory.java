package com.echoboard.util;

import com.echoboard.entity.Poll;
import com.echoboard.entity.Session;
import com.echoboard.enums.PollStatus;
import com.echoboard.enums.PollType;

import java.time.LocalDateTime;

public final class PollEntityFactory {

    private PollEntityFactory() {
    }

    public static Poll draftPoll() {
        return pollWithStatus(PollStatus.DRAFT);
    }

    public static Poll publishedPoll() {
        return pollWithStatus(PollStatus.PUBLISHED);
    }

    public static Poll closedPoll() {
        return pollWithStatus(PollStatus.CLOSED);
    }

    public static Poll pollWithStatus(PollStatus status) {
        Poll poll = new Poll();

        poll.setId(1L);
        poll.setSession(SessionEntityFactory.liveSession());
        poll.setTitle("Best backend topic?");
        poll.setStatus(status);
        poll.setType(PollType.SINGLE_CHOICE);
        poll.setCreatedAt(LocalDateTime.now().minusMinutes(30));

        if (status == PollStatus.PUBLISHED || status == PollStatus.CLOSED) {
            poll.setPublishedAt(LocalDateTime.now().minusMinutes(20));
        }

        if (status == PollStatus.CLOSED) {
            poll.setClosedAt(LocalDateTime.now().minusMinutes(5));
        }

        return poll;
    }

    public static Poll publishedPollForSession(Session session) {
        Poll poll = publishedPoll();

        poll.setSession(session);

        return poll;
    }
}