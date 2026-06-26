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
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
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

                simpMessagingTemplate.convertAndSend(
                        "/topic/chat/" + chatMessage.getChatRoom().getId(),
                        response
                );
            }
        });
    }

    public void publishScheduleUpdated(List<ChatMessage> chatMessages) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                for (ChatMessage chatMessage : chatMessages) {
                    ChatMessageResponse response = ChatMessageResponse.from(
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
                    );

                    simpMessagingTemplate.convertAndSend(
                            "/topic/chat/" + chatMessage.getChatRoom().getId(),
                            response
                    );
                }
            }
        });
    }

    public void publishScheduleShareCanceled(ChatMessage chatMessage, int unreadCount) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
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

                simpMessagingTemplate.convertAndSend(
                        "/topic/chat/" + chatMessage.getChatRoom().getId(),
                        response
                );
            }
        });
    }

    public void publishSharedScheduleDeleted(List<ChatMessage> chatMessages) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                for (ChatMessage chatMessage : chatMessages) {
                    ChatMessageResponse response = ChatMessageResponse.from(
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
                    );

                    simpMessagingTemplate.convertAndSend(
                            "/topic/chat/" + chatMessage.getChatRoom().getId(),
                            response
                    );
                }
            }
        });
    }
}
