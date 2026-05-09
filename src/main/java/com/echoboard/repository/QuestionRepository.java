package com.echoboard.repository;

import com.echoboard.entity.Question;
import com.echoboard.enums.QuestionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    Page<Question> findBySession_IdAndStatus(
            Long sessionId,
            QuestionStatus status,
            Pageable pageable
    );

    Optional<Question> findByIdAndSession_IdAndParticipant_Id(Long id, Long sessionId, Long participantId);

    long countBySession_IdAndStatus(Long sessionId, QuestionStatus status);

    List<Question> findTop5BySession_IdAndStatusInOrderByUpvoteCountDescCreatedAtAsc(
            Long sessionId,
            Collection<QuestionStatus> statuses
    );

    List<Question> findBySession_IdOrderByCreatedAtAsc(Long sessionId);


}
