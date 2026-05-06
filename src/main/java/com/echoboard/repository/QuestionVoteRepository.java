package com.echoboard.repository;

import com.echoboard.entity.QuestionVote;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionVoteRepository extends JpaRepository<QuestionVote, Long> {

    boolean existsByParticipant_IdAndQuestion_Id(Long participantId, Long questionId);
}
