package com.example.schedulebook.domain.schedule.event;

import com.example.schedulebook.common.executor.AfterCommitExecutor;
import com.example.schedulebook.common.websocket.WebSocketPublisher;
import com.example.schedulebook.domain.chat.dto.response.ChatMessageResponse;
import com.example.schedulebook.domain.chat.entity.ChatMessage;
import com.example.schedulebook.domain.schedule.dto.response.SchedulePreviewResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.example.schedulebook.common.consts.WebSocketDestination.chat;

@Component
@RequiredArgsConstructor
@Slf4j
public class ScheduleSharePublisher {
    private final AfterCommitExecutor afterCommitExecutor;
    private final WebSocketPublisher webSocketPublisher;

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

    public void publishAcceptSharedSchedule(ChatMessage chatMessage) {
        ChatMessageResponse response = createSchedulePreviewResponse(chatMessage, false, false, true, 0);

        publishResponses(response);
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
        List<ChatMessageResponse> responses = createResponses(chatMessages, true, false, false);

        publishResponses(responses);
    }

    private void publishResponses(ChatMessageResponse response) {
        webSocketPublisher.sendAfterCommit(
                chat(response.roomId()),
                response
        );
    }

    private void publishResponses(List<ChatMessageResponse> responses) {
        if (responses.isEmpty()) {
            return;
        }

        afterCommitExecutor.execute(() -> {
            responses.forEach(response ->
                webSocketPublisher.send(
                        chat(response.roomId()),
                        response)
            );
        });
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
}
