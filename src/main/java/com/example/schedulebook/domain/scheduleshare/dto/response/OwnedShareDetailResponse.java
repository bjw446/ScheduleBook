package com.example.schedulebook.domain.scheduleshare.dto.response;

import com.example.schedulebook.domain.scheduleparticipant.dto.response.ScheduleParticipantResponse;
import com.example.schedulebook.domain.scheduleshare.entity.ScheduleShare;

import java.time.LocalDate;
import java.util.List;

public record OwnedShareDetailResponse(
        Long shareId,
        Long scheduleId,
        String title,
        String content,
        LocalDate scheduleDate,
        Long sharedUserId,
        String sharedUserNickname,
        boolean participated,
        int participantCount,
        List<ScheduleParticipantResponse> participants
) {
    public static OwnedShareDetailResponse from(
            ScheduleShare scheduleShare,
            boolean participated,
            int participantCount,
            List<ScheduleParticipantResponse> participants
    ) {
        return new OwnedShareDetailResponse(
                scheduleShare.getId(),
                scheduleShare.getSchedule().getId(),
                scheduleShare.getSchedule().getTitle(),
                scheduleShare.getSchedule().getContent(),
                scheduleShare.getSchedule().getScheduleDate(),
                scheduleShare.getSharedUser().getId(),
                scheduleShare.getSharedUser().getNickname(),
                participated,
                participantCount,
                participants
        );
    }
}
