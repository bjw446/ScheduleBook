package com.example.schedulebook.domain.schedule.service;

import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.domain.chat.entity.ChatMessage;
import com.example.schedulebook.domain.schedule.entity.ScheduleSnapshot;
import com.example.schedulebook.domain.schedule.entity.ScheduleSnapshotHistory;
import com.example.schedulebook.domain.schedule.repository.ScheduleSnapshotHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Component
@RequiredArgsConstructor
public class ScheduleSnapshotHistoryManager {
    private final ScheduleSnapshotHistoryRepository scheduleSnapshotHistoryRepository;

    public void save(ChatMessage chatMessage) {
        scheduleSnapshotHistoryRepository.save(ScheduleSnapshotHistory.of(chatMessage));
    }

    @Transactional(readOnly = true)
    public ScheduleSnapshot findSnapshot(ChatMessage chatMessage, Long version) {
        if (Objects.equals(chatMessage.currentSnapshot().getScheduleVersion(), version)) {
            return chatMessage.currentSnapshot();
        }

        return scheduleSnapshotHistoryRepository.findByChatMessageIdAndVersion(chatMessage.getId(), version)
                .map(ScheduleSnapshotHistory::getScheduleSnapshot)
                .orElseThrow(() ->
                        new BaseException(ErrorEnum.SCHEDULE_SNAPSHOT_NOT_FOUND)
                );
    }
}
