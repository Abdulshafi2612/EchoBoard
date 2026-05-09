package com.echoboard.repository;

import com.echoboard.entity.Participant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParticipantRepository extends JpaRepository<Participant, Long> {

    long countBySession_Id(Long sessionId);

}
