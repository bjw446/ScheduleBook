package com.example.schedulebook.domain.schedule.event;

import com.example.schedulebook.domain.chat.dto.response.ChatMessageResponse;
import com.example.schedulebook.domain.chat.entity.ChatMessage;
import com.example.schedulebook.domain.schedule.dto.response.SchedulePreviewResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ScheduleSharePublisher {
    private final SimpMessagingTemplate simpMessagingTemplate;

    public void publishScheduleShared(ChatMessage chatMessage, int unreadCount) {
        ChatMessageResponse response = ChatMessageResponse.from(
                chatMessage,
                unreadCount,
                SchedulePreviewResponse.from(
                        chatMessage.getId(),
                        chatMessage.getScheduleId(),
                        chatMessage.getScheduleSnapshot(),
                        false,
                        false,
                        false
                )
        );

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                simpMessagingTemplate.convertAndSend(
                        "/topic/chat/" + chatMessage.getChatRoom().getId(),
                        response
                );
            }
        });
    }

    public void publishScheduleUpdated(List<ChatMessage> chatMessages) {
        List<ChatMessageResponse> responses = chatMessages
                .stream()
                .map(chatMessage ->
                        ChatMessageResponse.from(
                                chatMessage,
                                0,
                                SchedulePreviewResponse.from(
                                        chatMessage.getId(),
                                        chatMessage.getScheduleId(),
                                        chatMessage.getScheduleSnapshot(),
                                        false,
                                        false,
                                        true
                                )
                        ))
                .toList();

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                for (ChatMessageResponse response : responses) {
                    simpMessagingTemplate.convertAndSend(
                            "/topic/chat/" + response.roomId(),
                            response
                    );
                }
            }
        });
    }

    public void publishScheduleShareCanceled(ChatMessage chatMessage, int unreadCount) {
        ChatMessageResponse response = ChatMessageResponse.from(
                chatMessage,
                unreadCount,
                SchedulePreviewResponse.from(
                        chatMessage.getId(),
                        chatMessage.getScheduleId(),
                        chatMessage.getScheduleSnapshot(),
                        false,
                        true,
                        false
                )
        );

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                simpMessagingTemplate.convertAndSend(
                        "/topic/chat/" + chatMessage.getChatRoom().getId(),
                        response
                );
            }
        });
    }

    public void publishSharedScheduleDeleted(List<ChatMessage> chatMessages) {
        List<ChatMessageResponse> responses = chatMessages
                .stream()
                .map(chatMessage ->
                        ChatMessageResponse.from(
                                chatMessage,
                                0,
                                SchedulePreviewResponse.from(
                                        chatMessage.getId(),
                                        chatMessage.getScheduleId(),
                                        chatMessage.getScheduleSnapshot(),
                                        true,
                                        true,
                                        false
                                )
                        ))
                .toList();

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                for (ChatMessageResponse response : responses) {
                    simpMessagingTemplate.convertAndSend(
                            "/topic/chat/" + response.roomId(),
                            response
                    );
                }
            }
        });
    }
}
