package com.example.schedulebook.domain.schedule.service;

import com.example.schedulebook.domain.chat.dto.request.PublishChatMessage;
import com.example.schedulebook.domain.chat.entity.ChatMessage;
import com.example.schedulebook.domain.chat.entity.ChatRoom;
import com.example.schedulebook.domain.chat.enums.SystemMessageType;
import com.example.schedulebook.domain.chat.event.ChatMessagePublisher;
import com.example.schedulebook.domain.chat.projection.MemberReadStatusProjection;
import com.example.schedulebook.domain.chat.repository.ChatMessageRepository;
import com.example.schedulebook.domain.chat.repository.ChatRoomMemberRepository;
import com.example.schedulebook.domain.chat.service.ChatMessageManager;
import com.example.schedulebook.domain.chat.service.ChatUnreadCountManager;
import com.example.schedulebook.domain.schedule.entity.Schedule;
import com.example.schedulebook.domain.schedule.event.ScheduleSharePublisher;
import com.example.schedulebook.domain.schedule.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
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
    private final ChatUnreadCountManager chatUnreadCountManager;
    private final ChatRoomMemberRepository chatRoomMemberRepository;

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

        SystemMessageType systemMessageType = SystemMessageType.SCHEDULE_UPDATED;

        List<PublishChatMessage> publishChatMessages = createPublishChatMessages(updatedMessages, systemMessageType);

        scheduleSharePublisher.publishScheduleUpdated(updatedMessages);

        chatMessagePublisher.publishMessages(publishChatMessages);
    }

    public void handleCanceled(Long scheduleId) {
        List<ChatMessage> chatMessages = chatMessageRepository.findAllByScheduleIdAndDeletedFalse(scheduleId)
                .stream()
                .filter(cm -> !cm.isScheduleShareCanceled())
                .toList();

        for (ChatMessage chatMessage : chatMessages) {
            chatMessage.cancelScheduleShare();
        }

        SystemMessageType systemMessageType = SystemMessageType.SCHEDULE_SHARE_CANCELED;

        List<PublishChatMessage> publishChatMessages = createPublishChatMessages(chatMessages, systemMessageType);

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

    private List<PublishChatMessage> createPublishChatMessages(List<ChatMessage> chatMessages, SystemMessageType systemMessageType) {
        Map<Long, ChatRoom> notifiedRooms = collectRooms(chatMessages);

        Map<Long, List<MemberReadStatusProjection>> readStatusMap = new HashMap<>();

        for (ChatRoom room : notifiedRooms.values()) {
            readStatusMap.put(
                    room.getId(),
                    chatRoomMemberRepository.findReadStatuses(room.getId())
            );
        }

        List<PublishChatMessage> publishChatMessages = notifiedRooms.values()
                .stream()
                .map(room -> {
                    ChatMessage systemMessage = chatMessageManager.createSystemMessage(room, systemMessageType);

                    List<MemberReadStatusProjection> readStatuses = readStatusMap.get(room.getId());

                    int unreadCount = chatUnreadCountManager.calculateUnreadCount(systemMessage, readStatuses);

                    return new PublishChatMessage(systemMessage, unreadCount);
                })
                .toList();

        return publishChatMessages;
    }
}
