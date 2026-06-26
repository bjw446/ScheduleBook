package com.example.schedulebook.domain.schedule.dto.response;

import com.example.schedulebook.domain.chat.entity.ChatMessage;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record SchedulePreviewDetailResponse(
        Long messageId,
        Long scheduleId,
        String title,
        String content,
        LocalDate scheduleDate,
        LocalTime startTime,
        LocalTime endTime,
        boolean deleted,
        boolean canceled,
        boolean edited,
        Long scheduleVersion,
        LocalDateTime scheduleUpdatedAt
) {
    public static SchedulePreviewDetailResponse from(
            ChatMessage chatMessage,
            boolean deleted,
            boolean edited
    ) {
        return new SchedulePreviewDetailResponse(
                chatMessage.getId(),
                chatMessage.getScheduleId(),
                chatMessage.getScheduleSnapshot().getTitle(),
                chatMessage.getScheduleSnapshot().getContent(),
                chatMessage.getScheduleSnapshot().getScheduleDate(),
                chatMessage.getScheduleSnapshot().getStartTime(),
                chatMessage.getScheduleSnapshot().getEndTime(),
                deleted,
                chatMessage.isScheduleShareCanceled(),
                edited,
                chatMessage.getScheduleSnapshot().getScheduleVersion(),
                chatMessage.getScheduleSnapshot().getScheduleUpdatedAt()
        );
    }
}
