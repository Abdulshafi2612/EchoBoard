package com.echoboard.mapper;

import com.echoboard.dto.question.QuestionAttachmentResponse;
import com.echoboard.entity.QuestionAttachment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface QuestionAttachmentMapper {

    @Mapping(target = "questionId", source = "question.id")
    QuestionAttachmentResponse questionAttachmentToQuestionAttachmentResponse(
            QuestionAttachment questionAttachment
    );
}