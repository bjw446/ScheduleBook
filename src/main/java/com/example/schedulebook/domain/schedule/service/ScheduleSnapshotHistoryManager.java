package com.example.schedulebook.domain.schedule.service;

import com.example.schedulebook.domain.chat.entity.ChatMessage;
import com.example.schedulebook.domain.schedule.entity.ScheduleSnapshotHistory;
import com.example.schedulebook.domain.schedule.repository.ScheduleSnapshotHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ScheduleSnapshotHistoryManager {
    private final ScheduleSnapshotHistoryRepository scheduleSnapshotHistoryRepository;

    public void save(ChatMessage chatMessage) {
        scheduleSnapshotHistoryRepository.save(ScheduleSnapshotHistory.of(chatMessage, chatMessage.getScheduleSnapshot()));
    }
}
