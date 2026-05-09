package com.echoboard.util;

import com.echoboard.dto.question.QuestionResponse;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.IOException;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.List;

public class CsvExportUtil {

    private CsvExportUtil() {
    }

    public static String toQuestionsCsv(List<QuestionResponse> questions) {
        try (StringWriter writer = new StringWriter();
             CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT)) {

            printQuestionHeader(csvPrinter);

            for (QuestionResponse question : questions) {
                printQuestionRow(csvPrinter, question);
            }

            csvPrinter.flush();
            return writer.toString();

        } catch (IOException exception) {
            throw new RuntimeException("Failed to export questions CSV", exception);
        }
    }

    private static void printQuestionHeader(CSVPrinter csvPrinter) throws IOException {
        csvPrinter.printRecord(
                "id",
                "sessionId",
                "participantDisplayName",
                "content",
                "status",
                "upvoteCount",
                "pinned",
                "answered",
                "createdAt",
                "approvedAt",
                "answeredAt"
        );
    }

    private static void printQuestionRow(CSVPrinter csvPrinter, QuestionResponse question) throws IOException {
        csvPrinter.printRecord(
                question.getId(),
                question.getSessionId(),
                question.getParticipantDisplayName(),
                question.getContent(),
                question.getStatus(),
                question.getUpvoteCount(),
                question.isPinned(),
                question.isAnswered(),
                formatDateTime(question.getCreatedAt()),
                formatDateTime(question.getApprovedAt()),
                formatDateTime(question.getAnsweredAt())
        );
    }

    private static String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }

        return dateTime.toString();
    }
}