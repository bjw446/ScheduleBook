package com.example.schedulebook.domain.comment.dto.response;

import com.example.schedulebook.domain.schedule.entity.Schedule;

import java.util.List;

public record ScheduleCommentListResponse(
        Long scheduleId,
        int commentCount,
        List<ScheduleCommentResponse> comments
) {
    public static ScheduleCommentListResponse from(Schedule schedule, List<ScheduleCommentResponse> comments) {
        return new ScheduleCommentListResponse(
                schedule.getId(),
                schedule.getCommentCount(),
                comments
        );
    }
}
