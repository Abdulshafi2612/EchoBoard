package com.echoboard.repository;

import com.echoboard.entity.Poll;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PollRepository extends JpaRepository<Poll, Long> {

    long countBySession_Id(Long sessionId);

    List<Poll> findBySession_IdOrderByCreatedAtAsc(Long sessionId);

}
