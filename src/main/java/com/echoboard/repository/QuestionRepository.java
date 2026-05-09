package com.echoboard.repository;

import com.echoboard.entity.Question;
import com.echoboard.enums.QuestionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    Page<Question> findBySession_IdAndStatus(
            Long sessionId,
            QuestionStatus status,
            Pageable pageable
    );

    Optional<Question> findByIdAndSession_IdAndParticipant_Id(Long id, Long sessionId, Long participantId);

    long countBySession_IdAndStatus(Long sessionId, QuestionStatus status);


}
