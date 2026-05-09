package com.echoboard.repository;

import com.echoboard.entity.Session;
import com.echoboard.entity.User;
import com.echoboard.enums.SessionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


public interface SessionRepository extends JpaRepository<Session, Long> {

    boolean existsByAccessCode(String accessCode);

    Optional<Session> findByAccessCode(String accessCode);

    Page<Session> findByOwner(User owner, Pageable pageable);

    Optional<Session> findByIdAndOwner(Long id, User owner);

    List<Session> findByStatusAndEndedAtBefore(SessionStatus status, LocalDateTime cutoff);

    List<Session> findByStatusAndStartedAtBefore(SessionStatus status, LocalDateTime cutoff);
}
