package com.example.schedulebook.domain.schedule.service;

import com.example.schedulebook.domain.schedule.dto.response.ScheduleParticipantInfo;
import com.example.schedulebook.domain.schedule.dto.response.ScheduleParticipantListResponse;
import com.example.schedulebook.domain.schedule.dto.response.ScheduleParticipantResponse;
import com.example.schedulebook.domain.schedule.projection.ScheduleParticipantProjection;
import com.example.schedulebook.domain.schedule.repository.ScheduleParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ScheduleParticipantReader {
    private final ScheduleParticipantRepository scheduleParticipantRepository;

    public ScheduleParticipantInfo getParticipantInfo(Long scheduleId, Long currentUserId) {
        List<ScheduleParticipantProjection> participants = scheduleParticipantRepository.findParticipants(scheduleId);

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
        List<ScheduleParticipantProjection> participants = scheduleParticipantRepository.findParticipants(scheduleId);

        return ScheduleParticipantListResponse.from(
                participants.size(),
                participants.stream()
                        .map(ScheduleParticipantResponse::from)
                        .toList()
        );
    }
}
