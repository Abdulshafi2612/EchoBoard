package com.echoboard.util;

import com.echoboard.entity.Question;
import com.echoboard.entity.QuestionAttachment;
import com.echoboard.enums.FileType;

import java.time.LocalDateTime;

public final class QuestionAttachmentEntityFactory {

    private QuestionAttachmentEntityFactory() {
    }

    public static QuestionAttachment attachmentForQuestion(Question question) {
        return QuestionAttachment
                .builder()
                .id(1L)
                .question(question)
                .fileName("attachment.pdf")
                .fileType(FileType.PDF)
                .fileSize(1024)
                .uploadedAt(LocalDateTime.now().minusMinutes(1))
                .fileUrl("uploads/sessions/1/questions/1/attachment.pdf")
                .build();
    }
}