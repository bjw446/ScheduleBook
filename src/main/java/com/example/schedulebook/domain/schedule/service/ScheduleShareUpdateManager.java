package com.example.schedulebook.domain.schedule.service;

import com.example.schedulebook.domain.chat.entity.ChatMessage;
import com.example.schedulebook.domain.chat.entity.ChatRoom;
import com.example.schedulebook.domain.chat.event.ChatMessagePublisher;
import com.example.schedulebook.domain.chat.repository.ChatMessageRepository;
import com.example.schedulebook.domain.chat.service.ChatMessageManager;
import com.example.schedulebook.domain.schedule.entity.Schedule;
import com.example.schedulebook.domain.schedule.event.ScheduleSharePublisher;
import com.example.schedulebook.domain.schedule.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class ScheduleShareUpdateManager {
    private final ScheduleRepository scheduleRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ScheduleSnapshotManager scheduleSnapshotManager;
    private final ChatMessageManager chatMessageManager;
    private final ScheduleSharePublisher scheduleSharePublisher;
    private final ChatMessagePublisher chatMessagePublisher;

    public void handleUpdated(Long scheduleId) {
        Schedule schedule = scheduleRepository.findById(scheduleId).orElseThrow();

        List<ChatMessage> sharedMessages =
                chatMessageRepository.findAllByScheduleIdAndDeletedFalse(scheduleId)
                        .stream()
                        .filter(message -> !message.isScheduleShareCanceled())
                        .toList();

        List<ChatMessage> updatedMessages =
                sharedMessages.stream()
                        .filter(message ->
                                scheduleSnapshotManager.updateSnapshot(message, schedule))
                        .toList();

        Map<Long, ChatRoom> notifiedRooms = collectRooms(updatedMessages);

        List<ChatMessage> systemMessages = notifiedRooms.values()
                        .stream()
                        .map(chatMessageManager::createScheduleUpdatedMessage)
                        .toList();

        scheduleSharePublisher.publishScheduleUpdated(updatedMessages);

        chatMessagePublisher.publishMessages(systemMessages);
    }

    public void handleCanceled(Long scheduleId) {
        List<ChatMessage> chatMessages = chatMessageRepository.findAllByScheduleIdAndDeletedFalse(scheduleId)
                .stream()
                .filter(cm -> !cm.isScheduleShareCanceled())
                .toList();

        for (ChatMessage chatMessage : chatMessages) {
            chatMessage.cancelScheduleShare();
        }

        Map<Long, ChatRoom> notifiedRooms = collectRooms(chatMessages);

        List<ChatMessage> systemMessages = notifiedRooms.values()
                .stream()
                .map(chatMessageManager::createScheduleCanceledMessage)
                .toList();

        scheduleSharePublisher.publishScheduleShareCanceled(chatMessages);

        chatMessagePublisher.publishMessages(systemMessages);
    }

    private Map<Long, ChatRoom> collectRooms(List<ChatMessage> messages) {

        Map<Long, ChatRoom> rooms = new LinkedHashMap<>();

        for (ChatMessage message : messages) {
            rooms.putIfAbsent(
                    message.getChatRoom().getId(),
                    message.getChatRoom()
            );
        }

        return rooms;
    }
}
