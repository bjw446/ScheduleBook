package com.example.schedulebook.domain.schedule.event;

import com.example.schedulebook.domain.chat.entity.ChatMessage;
import com.example.schedulebook.domain.chat.repository.ChatMessageRepository;
import com.example.schedulebook.domain.schedule.entity.Schedule;
import com.example.schedulebook.domain.schedule.repository.ScheduleRepository;
import com.example.schedulebook.domain.schedule.service.ScheduleSnapshotManager;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ScheduleShareEventListener {
    private final ChatMessageRepository chatMessageRepository;
    private final ScheduleRepository scheduleRepository;
    private final ScheduleSharePublisher scheduleSharePublisher;
    private final ScheduleSnapshotManager scheduleSnapshotManager;


    @EventListener
    public void handleScheduleUpdated(ScheduleUpdatedEvent event) {
        Schedule schedule = scheduleRepository.findById(event.scheduleId()).orElseThrow();

        List<ChatMessage> chatMessages = chatMessageRepository.findAllByScheduleIdAndDeletedFalse(event.scheduleId())
                .stream()
                .filter(cm -> !cm.isScheduleShareCanceled())
                .filter(cm -> scheduleSnapshotManager.updateSnapshot(cm, schedule))
                .toList();

        scheduleSharePublisher.publishScheduleUpdated(chatMessages);
    }

    @EventListener
    public void handleScheduleCanceled(ScheduleDeletedEvent event) {
        List<ChatMessage> chatMessages = chatMessageRepository.findAllByScheduleIdAndDeletedFalse(event.scheduleId())
                .stream()
                .filter(cm -> !cm.isScheduleShareCanceled())
                .toList();

        for (ChatMessage chatMessage : chatMessages) {
            chatMessage.cancelScheduleShare();
        }

        scheduleSharePublisher.publishSharedScheduleDeleted(chatMessages);
    }
}
