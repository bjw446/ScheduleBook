package com.example.schedulebook.domain.schedule.dto.response;

import java.util.List;

public record ScheduleParticipantListResponse(
        int participantCount,
        List<ScheduleParticipantResponse> participants
) {
    public static ScheduleParticipantListResponse from(
            int participantCount,
            List<ScheduleParticipantResponse> participants
    ) {
        return new ScheduleParticipantListResponse(
                participantCount,
                participants
        );
    }
}
