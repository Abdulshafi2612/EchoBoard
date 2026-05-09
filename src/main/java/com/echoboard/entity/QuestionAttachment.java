package com.echoboard.entity;

import com.echoboard.enums.FileType;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "question_attachments")
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(nullable = false)
    @Size(min = 1, max = 100)
    private String fileName;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private FileType fileType;

    @Column(nullable = false)
    @Max(1024 * 1024 * 2)
    private long fileSize;

    @Column(nullable = false)
    private LocalDateTime uploadedAt;

    @Column(nullable = false)
    private String fileUrl;


    @PrePersist
    public void prePersist() {
        uploadedAt = LocalDateTime.now();
    }
}
