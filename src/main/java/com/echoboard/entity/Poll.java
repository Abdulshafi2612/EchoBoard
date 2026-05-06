package com.echoboard.entity;

import com.echoboard.enums.PollStatus;
import com.echoboard.enums.PollType;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "polls")
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Poll {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private Session session;

    @Column(nullable = false)
    @Size(min = 1, max = 200)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PollStatus status = PollStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PollType type;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime publishedAt;

    private  LocalDateTime closedAt;


    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
