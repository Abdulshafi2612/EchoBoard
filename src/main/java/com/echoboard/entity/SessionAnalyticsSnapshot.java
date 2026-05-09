package com.echoboard.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "session_analytics_snapshots")
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SessionAnalyticsSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false, unique = true)
    private Session session;

    @Column(nullable = false)
    private long totalParticipants;

    @Column(nullable = false)
    private long totalQuestions;

    @Column(nullable = false)
    private long pendingQuestions;

    @Column(nullable = false)
    private long approvedQuestions;

    @Column(nullable = false)
    private long answeredQuestions;

    @Column(nullable = false)
    private long hiddenQuestions;

    @Column(nullable = false)
    private long totalPolls;

    @Column(nullable = false)
    private long totalPollVotes;

    @Column(nullable = false)
    private LocalDateTime generatedAt;

    @PrePersist
    public void prePersist() {
        this.generatedAt = LocalDateTime.now();
    }
}