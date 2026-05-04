package com.echoboard.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "participants")
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Participant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "session_id", nullable = false)
    private Session session;

    private String displayName;

    @Column(unique = true)
    private String participantTokenHash;

    private LocalDateTime mutedUntil;

    @Column(nullable = false, updatable = false)
    private LocalDateTime joinedAt;

    @Column(nullable = false)
    private LocalDateTime lastSeenAt;

    @PrePersist
    protected void onJoin() {
        LocalDateTime now = LocalDateTime.now();
        this.joinedAt = now;
        this.lastSeenAt = now;
    }

}
