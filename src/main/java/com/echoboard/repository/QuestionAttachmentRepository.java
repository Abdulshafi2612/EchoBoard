package com.echoboard.repository;

import com.echoboard.entity.QuestionAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionAttachmentRepository extends JpaRepository<QuestionAttachment, Long> {
}
