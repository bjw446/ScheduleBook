package com.example.schedulebook.domain.schedule_participant.service;

import com.example.schedulebook.domain.schedule_participant.dto.response.ScheduleParticipantInfo;
import com.example.schedulebook.domain.schedule_participant.dto.response.ScheduleParticipantListResponse;
import com.example.schedulebook.domain.schedule_participant.dto.response.ScheduleParticipantResponse;
import com.example.schedulebook.domain.schedule_participant.projection.ScheduleParticipantProjection;
import com.example.schedulebook.domain.schedule_participant.repository.ScheduleParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ScheduleParticipantReader {
    private final ScheduleParticipantRepository scheduleParticipantRepository;

    public ScheduleParticipantInfo getParticipantInfo(Long scheduleId, Long currentUserId) {
        List<ScheduleParticipantProjection> participants = projectionList(scheduleId);

        boolean participated = participants.stream()
                .anyMatch(p ->
                        p.getUserId().equals(currentUserId)
                );

        return ScheduleParticipantInfo.from(
                participated,
                participants.size(),
                participants.stream()
                        .map(ScheduleParticipantResponse::from)
                        .toList()
        );
    }

    public ScheduleParticipantListResponse getParticipantList(Long scheduleId) {
        List<ScheduleParticipantProjection> participants = projectionList(scheduleId);

        return ScheduleParticipantListResponse.from(
                participants.size(),
                participants.stream()
                        .map(ScheduleParticipantResponse::from)
                        .toList()
        );
    }

    private List<ScheduleParticipantProjection> projectionList(Long scheduleId) {
        return scheduleParticipantRepository.findParticipants(scheduleId);
    }
}
