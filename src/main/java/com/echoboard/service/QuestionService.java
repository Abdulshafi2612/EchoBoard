package com.echoboard.service;

import com.echoboard.dto.common.PageResponse;
import com.echoboard.dto.question.QuestionAttachmentResponse;
import com.echoboard.dto.question.QuestionResponse;
import com.echoboard.dto.question.SubmitQuestionRequest;
import com.echoboard.enums.QuestionStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface QuestionService {

    PageResponse<QuestionResponse> getQuestionsBySessionIdAndStatus(Pageable pageable, Long sessionId, QuestionStatus status);

    QuestionResponse submitQuestion(Long sessionId, SubmitQuestionRequest request);

    void deleteQuestion(Long sessionId, Long questionId);

    void approveQuestion(Long sessionId, Long questionId);

    void hideQuestion(Long sessionId, Long questionId);

    void pinQuestion(Long sessionId, Long questionId);

    void unpinquestion(Long sessionId, Long questionId);

    void markQuestionAsAnswered(Long sessionId, Long questionId);

    void upvotequestion(Long sessionId, Long questionId);

    long getNumberOfQuestionsBySessionIdAndStatus(Long sessionId, QuestionStatus status);

    List<QuestionResponse> getTopUpvotedQuestionBySessionId(Long sessionId);

    List<QuestionResponse> getQuestionsForExportBySessionId(Long sessionId);

    QuestionAttachmentResponse uploadQuestionAttachment(Long sessionId, Long questionId, MultipartFile file);
}