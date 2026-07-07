package com.example.schedulebook.domain.scheduleshare.dto.response;

import com.example.schedulebook.domain.scheduleparticipant.dto.response.ScheduleParticipantResponse;
import com.example.schedulebook.domain.scheduleshare.entity.ScheduleShare;

import java.time.LocalDate;
import java.util.List;

public record SharedScheduleDetailResponse(
        Long shareId,
        Long scheduleId,
        String title,
        String contents,
        LocalDate scheduleDate,
        String ownerNickname,
        boolean participated,
        int participantCount,
        List<ScheduleParticipantResponse> participants
) {
    public static SharedScheduleDetailResponse from(
            ScheduleShare scheduleShare,
            boolean participated,
            int participantCount,
            List<ScheduleParticipantResponse> participants
    ) {
        return new SharedScheduleDetailResponse(
                scheduleShare.getId(),
                scheduleShare.getSchedule().getId(),
                scheduleShare.getSchedule().getTitle(),
                scheduleShare.getSchedule().getContent(),
                scheduleShare.getSchedule().getScheduleDate(),
                scheduleShare.getSchedule().getUser().getNickname(),
                participated,
                participantCount,
                participants
        );
    }
}
