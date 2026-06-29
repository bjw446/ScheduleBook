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
        ChatMessageResponse response = createSchedulePreviewResponse(
                chatMessage,
                false,
                false,
                false,
                unreadCount
        );

        publishResponses(response);
    }

    public void publishScheduleUpdated(List<ChatMessage> chatMessages) {
        List<ChatMessageResponse> responses = createResponses(chatMessages, false, false, true);

        publishResponses(responses);
    }

    public void publishScheduleShareCanceled(ChatMessage chatMessage, int unreadCount) {
        ChatMessageResponse response = createSchedulePreviewResponse(
                chatMessage,
                false,
                true,
                false,
                unreadCount
        );

        publishResponses(response);
    }

    public void publishScheduleShareCanceled(List<ChatMessage> chatMessages) {
        List<ChatMessageResponse> responses = createResponses(chatMessages, false, true, false);

        publishResponses(responses);
    }

    public void publishSharedScheduleDeleted(List<ChatMessage> chatMessages) {
        List<ChatMessageResponse> responses = createResponses(chatMessages, true, true, false);

        publishResponses(responses);
    }

    private void publishResponses(ChatMessageResponse response) {
        afterCommit(() -> simpMessagingTemplate.convertAndSend(
                "/topic/chat/" + response.roomId(),
                response
        ));
    }

    private void publishResponses(List<ChatMessageResponse> responses) {
        if (responses.isEmpty()) {
            return;
        }

        afterCommit(() -> responses.forEach(response ->
                simpMessagingTemplate.convertAndSend(
                        "/topic/chat/" + response.roomId(),
                        response
                )
        ));
    }

    private ChatMessageResponse createSchedulePreviewResponse(
            ChatMessage chatMessage,
            boolean deleted,
            boolean canceled,
            boolean edited,
            int unreadCount
    ) {
        return ChatMessageResponse.from(
                chatMessage,
                unreadCount,
                SchedulePreviewResponse.from(
                        chatMessage.getId(),
                        chatMessage.getScheduleId(),
                        chatMessage.getScheduleSnapshot(),
                        deleted,
                        canceled,
                        edited
                )
        );
    }

    private List<ChatMessageResponse> createResponses(List<ChatMessage> chatMessages, boolean deleted, boolean canceled, boolean edited) {
        return chatMessages.stream()
                .map(message ->
                        createSchedulePreviewResponse(
                                message,
                                deleted,
                                canceled,
                                edited,
                                0
                        )
                )
                .toList();
    }

    private void afterCommit(Runnable action) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }
}
