package com.example.schedulebook.domain.scheduleshare.publisher;

import com.example.schedulebook.common.consts.WebSocketDestination;
import com.example.schedulebook.common.executor.AfterCommitExecutor;
import com.example.schedulebook.common.websocket.publisher.WebSocketPublisher;
import com.example.schedulebook.domain.chatmessage.dto.response.ChatMessageResponse;
import com.example.schedulebook.domain.chatmessage.entity.ChatMessage;
import com.example.schedulebook.domain.schedulesnapshot.dto.response.SchedulePreviewResponse;
import com.example.schedulebook.domain.schedulesnapshot.enums.SchedulePreviewState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;


@Component
@RequiredArgsConstructor
@Slf4j
public class ScheduleSharePublisher {
    private final AfterCommitExecutor afterCommitExecutor;
    private final WebSocketPublisher webSocketPublisher;

    public void publishScheduleShared(ChatMessage chatMessage, int unreadCount) {
        ChatMessageResponse response = createSchedulePreviewResponse(
                chatMessage,
                SchedulePreviewState.NORMAL,
                unreadCount
        );

        publishResponses(response);
    }

    public void publishScheduleUpdated(List<ChatMessage> chatMessages) {
        List<ChatMessageResponse> responses = createResponses(chatMessages, SchedulePreviewState.EDITED);

        publishResponses(responses);
    }

    public void publishAcceptSharedSchedule(ChatMessage chatMessage) {
        ChatMessageResponse response = createSchedulePreviewResponse(chatMessage, SchedulePreviewState.ACCEPTED, 0);

        publishResponses(response);
    }

    public void publishScheduleShareCanceled(ChatMessage chatMessage, int unreadCount) {
        ChatMessageResponse response = createSchedulePreviewResponse(
                chatMessage,
                SchedulePreviewState.CANCELED,
                unreadCount
        );

        publishResponses(response);
    }

    public void publishScheduleShareCanceled(List<ChatMessage> chatMessages) {
        List<ChatMessageResponse> responses = createResponses(chatMessages, SchedulePreviewState.CANCELED);

        publishResponses(responses);
    }

    public void publishSharedScheduleDeleted(List<ChatMessage> chatMessages) {
        List<ChatMessageResponse> responses = createResponses(chatMessages, SchedulePreviewState.DELETED);

        publishResponses(responses);
    }

    private void publishResponses(ChatMessageResponse response) {
        webSocketPublisher.sendAfterCommit(
                WebSocketDestination.getChatDestination(response.roomId()),
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
                        WebSocketDestination.getChatDestination(response.roomId()),
                        response)
            );
        });
    }

    private ChatMessageResponse createSchedulePreviewResponse(
            ChatMessage chatMessage,
            SchedulePreviewState schedulePreviewState,
            int unreadCount
    ) {
        return ChatMessageResponse.from(
                chatMessage,
                unreadCount,
                SchedulePreviewResponse.from(
                        chatMessage.getId(),
                        chatMessage.getScheduleId(),
                        chatMessage.getScheduleSnapshot(),
                        schedulePreviewState
                )
        );
    }

    private List<ChatMessageResponse> createResponses(List<ChatMessage> chatMessages, SchedulePreviewState schedulePreviewState) {
        return chatMessages.stream()
                .map(message ->
                        createSchedulePreviewResponse(
                                message,
                                schedulePreviewState,
                                0
                        )
                )
                .toList();
    }
}
