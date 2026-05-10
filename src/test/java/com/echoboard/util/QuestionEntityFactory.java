package com.echoboard.util;

import com.echoboard.entity.Participant;
import com.echoboard.entity.Question;
import com.echoboard.entity.Session;
import com.echoboard.enums.QuestionStatus;

import java.time.LocalDateTime;

public final class QuestionEntityFactory {

    private QuestionEntityFactory() {
    }

    public static Question pendingQuestion() {
        return questionWithStatus(QuestionStatus.PENDING);
    }

    public static Question approvedQuestion() {
        return questionWithStatus(QuestionStatus.APPROVED);
    }

    public static Question answeredQuestion() {
        Question question = questionWithStatus(QuestionStatus.ANSWERED);

        question.setAnswered(true);
        question.setAnsweredAt(LocalDateTime.now().minusMinutes(5));

        return question;
    }

    public static Question hiddenQuestion() {
        return questionWithStatus(QuestionStatus.HIDDEN);
    }

    public static Question questionWithStatus(QuestionStatus status) {
        Session session = SessionEntityFactory.liveSession();
        Participant participant = ParticipantEntityFactory.participantForSession(session);

        Question question = new Question();

        question.setId(1L);
        question.setSession(session);
        question.setParticipant(participant);
        question.setContent("What is the roadmap?");
        question.setStatus(status);
        question.setUpvoteCount(3);
        question.setPinned(false);
        question.setAnswered(false);
        question.setCreatedAt(LocalDateTime.now().minusMinutes(15));

        if (status == QuestionStatus.APPROVED || status == QuestionStatus.ANSWERED) {
            question.setApprovedAt(LocalDateTime.now().minusMinutes(10));
        }

        if (status == QuestionStatus.ANSWERED) {
            question.setAnswered(true);
            question.setAnsweredAt(LocalDateTime.now().minusMinutes(5));
        }

        return question;
    }

    public static Question approvedQuestionForSessionAndParticipant(Session session, Participant participant) {
        Question question = approvedQuestion();

        question.setSession(session);
        question.setParticipant(participant);

        return question;
    }
}