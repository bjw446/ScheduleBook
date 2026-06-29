package com.example.schedulebook.domain.schedule.service;

import com.example.schedulebook.domain.chat.dto.request.PublishChatMessage;
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

        List<ChatMessage> updatedMessages = chatMessageRepository.findAllByScheduleIdAndDeletedFalse(scheduleId)
                .stream()
                .filter(message -> !message.isScheduleShareCanceled())
                .filter(message -> scheduleSnapshotManager.updateSnapshot(message, schedule))
                .toList();


        List<PublishChatMessage> publishChatMessages = collectRooms(updatedMessages).values()
                .stream()
                .map(chatMessageManager::createScheduleUpdatedSystemMessage)
                .toList();

        scheduleSharePublisher.publishScheduleUpdated(updatedMessages);

        chatMessagePublisher.publishMessages(publishChatMessages);
    }

    public void handleCanceled(Long scheduleId) {
        List<ChatMessage> chatMessages = chatMessageRepository.findAllByScheduleIdAndDeletedFalse(scheduleId)
                .stream()
                .filter(cm -> !cm.isScheduleShareCanceled())
                .toList();

        chatMessages.forEach(ChatMessage::cancelScheduleShare);

        List<PublishChatMessage> publishChatMessages = collectRooms(chatMessages).values()
                .stream()
                .map(chatMessageManager::createScheduleShareCanceledSystemMessage)
                .toList();

        scheduleSharePublisher.publishScheduleShareCanceled(chatMessages);

        chatMessagePublisher.publishMessages(publishChatMessages);
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
