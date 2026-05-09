package com.echoboard.repository;

import com.echoboard.entity.SessionAnalyticsSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SessionAnalyticsSnapshotRepository extends JpaRepository<SessionAnalyticsSnapshot, Long> {

    Optional<SessionAnalyticsSnapshot> findBySession_Id(Long sessionId);

    boolean existsBySession_Id(Long sessionId);
}
