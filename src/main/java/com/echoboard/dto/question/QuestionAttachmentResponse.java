package com.echoboard.dto.question;

import com.echoboard.enums.FileType;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class QuestionAttachmentResponse {

    private Long id;

    private Long questionId;

    private String fileName;

    private String fileUrl;

    private FileType fileType;

    private long fileSize;

    private LocalDateTime uploadedAt;
}
