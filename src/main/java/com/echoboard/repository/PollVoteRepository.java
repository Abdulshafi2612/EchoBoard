package com.echoboard.repository;

import com.echoboard.entity.PollVote;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PollVoteRepository extends JpaRepository<PollVote, Long> {

    boolean existsByParticipant_IdAndPoll_Id(Long participantId, Long pollId);
}
