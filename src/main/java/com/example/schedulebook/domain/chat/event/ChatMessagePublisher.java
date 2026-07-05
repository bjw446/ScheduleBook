package com.example.schedulebook.domain.chat.event;

import com.example.schedulebook.common.executor.AfterCommitExecutor;
import com.example.schedulebook.common.websocket.WebSocketPublisher;
import com.example.schedulebook.domain.chat.dto.request.PublishChatMessage;
import com.example.schedulebook.domain.chat.dto.response.ChatMessageResponse;
import com.example.schedulebook.domain.chat.entity.ChatMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.example.schedulebook.common.consts.CommonConst.DELETED_MESSAGE;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatMessagePublisher {
    private final SimpMessagingTemplate simpMessagingTemplate;
    private final AfterCommitExecutor afterCommitExecutor;
    private final WebSocketPublisher webSocketPublisher;

    public void publishMessage(ChatMessage chatMessage, int unreadCount) {
        ChatMessageResponse response = ChatMessageResponse.from(chatMessage, unreadCount);

        webSocketPublisher.sendAfterCommit(  "/topic/chat/" + response.roomId(), response);
    }

    public void publishReadMessageAfterCommit(Long roomId, Long currentUserId, Long lastReadMessageId) {
        webSocketPublisher.sendAfterCommit(
                "/topic/chat/" + roomId + "/read",
                ReadMessageEvent.from(roomId, currentUserId, lastReadMessageId)
        );
    }

    public void publishDeleteMessageAfterCommit(Long roomId, Long messageId) {
        webSocketPublisher.sendAfterCommit(
                "/topic/chat/" + roomId + "/delete",
                new ChatMessageDeletedEvent(
                        roomId,
                        messageId,
                        DELETED_MESSAGE
                )
        );
    }

    public void publishMessages(List<PublishChatMessage> publishChatMessages) {
        List<ChatMessageResponse> responses =
                publishChatMessages.stream()
                        .map(item -> ChatMessageResponse.from(item.chatMessage(), item.unreadCount()))
                        .toList();

        afterCommitExecutor.execute(() -> {
            responses.forEach(response ->
                    webSocketPublisher.send(
                            "/topic/chat/" + response.roomId(),
                            response
                    )
            );
        });
    }
}
