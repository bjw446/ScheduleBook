package com.example.schedulebook.domain.schedule.service;

import com.example.schedulebook.domain.chat.entity.ChatMessage;
import com.example.schedulebook.domain.schedule.entity.Schedule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ScheduleSnapshotManager {
    private final ScheduleSnapshotHistoryManager scheduleSnapshotHistoryManager;

    public boolean updateSnapshot(ChatMessage chatMessage, Schedule schedule) {
        if (!chatMessage.needSnapshotUpdate(schedule)) {
            return false;
        }

        scheduleSnapshotHistoryManager.save(chatMessage);

        chatMessage.updateScheduleSnapshot(schedule);

        return true;
    }
}
