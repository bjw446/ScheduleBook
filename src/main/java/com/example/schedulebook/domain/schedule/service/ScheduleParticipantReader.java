package com.example.schedulebook.domain.schedule.service;

import com.example.schedulebook.domain.schedule.dto.response.ScheduleParticipantInfo;
import com.example.schedulebook.domain.schedule.dto.response.ScheduleParticipantResponse;
import com.example.schedulebook.domain.schedule.entity.Schedule;
import com.example.schedulebook.domain.schedule.entity.ScheduleParticipant;
import com.example.schedulebook.domain.schedule.repository.ScheduleParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ScheduleParticipantReader {
    private final ScheduleParticipantRepository scheduleParticipantRepository;

    public ScheduleParticipantInfo getParticipantInfo(Long ScheduleId, Long currentUserId) {
        List<ScheduleParticipant> participants = scheduleParticipantRepository.findParticipants(ScheduleId);

        boolean participated = participants.stream()
                .anyMatch(p ->
                        p.getUser().getId().equals(currentUserId)
                );

        return ScheduleParticipantInfo.from(
                participated,
                participants.size(),
                participants.stream()
                        .map(ScheduleParticipantResponse::from)
                        .toList()
        );
    }
}
