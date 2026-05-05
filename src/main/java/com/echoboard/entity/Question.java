package com.echoboard.entity;

import com.echoboard.enums.QuestionStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "questions")
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "session_id", nullable = false)
    private Session session;

    @ManyToOne
    @JoinColumn(name = "participant_id", nullable = false)
    private Participant participant;

    @Column(nullable = false)
    @Size(min = 1, max = 1000)
    private String content;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QuestionStatus status;


    private boolean pinned = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime approvedAt;

    private LocalDateTime answeredAt;


    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
