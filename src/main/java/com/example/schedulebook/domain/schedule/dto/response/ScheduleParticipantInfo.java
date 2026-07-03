package com.example.schedulebook.domain.schedule.dto.response;

import java.util.List;

public record ScheduleParticipantInfo(
        boolean participated,
        int participantCount,
        List<ScheduleParticipantResponse> participants
) {
    public static ScheduleParticipantInfo from(
            boolean participated,
            int participantCount,
            List<ScheduleParticipantResponse> participants
    ) {
        return new ScheduleParticipantInfo(
                participated,
                participantCount,
                participants
        );
    }
}
