package com.echoboard.util;

import com.echoboard.entity.Poll;
import com.echoboard.entity.PollOption;

public final class PollOptionEntityFactory {

    private PollOptionEntityFactory() {
    }

    public static PollOption option() {
        return optionForPoll(PollEntityFactory.publishedPoll());
    }

    public static PollOption optionForPoll(Poll poll) {
        PollOption option = new PollOption();

        option.setId(1L);
        option.setPoll(poll);
        option.setText("Spring Boot");
        option.setVoteCount(5);

        return option;
    }

    public static PollOption optionForPoll(Long id, Poll poll, String text, int voteCount) {
        PollOption option = new PollOption();

        option.setId(id);
        option.setPoll(poll);
        option.setText(text);
        option.setVoteCount(voteCount);

        return option;
    }
}