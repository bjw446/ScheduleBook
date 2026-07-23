package com.example.schedulebook.domain.chatmessage.publisher;

import com.example.schedulebook.common.consts.CommonConst;
import com.example.schedulebook.common.consts.WebSocketDestination;
import com.example.schedulebook.common.executor.AfterCommitExecutor;
import com.example.schedulebook.common.websocket.publisher.WebSocketPublisher;
import com.example.schedulebook.domain.chatmessage.dto.request.PublishChatMessage;
import com.example.schedulebook.domain.chatmessage.dto.response.ChatMessageResponse;
import com.example.schedulebook.domain.chatmessage.entity.ChatMessage;
import com.example.schedulebook.domain.chatmessage.event.ChatMessageDeletedEvent;
import com.example.schedulebook.domain.chatmessage.event.ReadMessageEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;


@Component
@RequiredArgsConstructor
@Slf4j
public class ChatMessagePublisher {
    private final AfterCommitExecutor afterCommitExecutor;
    private final WebSocketPublisher webSocketPublisher;

    public void publishMessage(ChatMessage chatMessage, int unreadCount) {
        ChatMessageResponse response = ChatMessageResponse.from(chatMessage, unreadCount);

        webSocketPublisher.sendAfterCommit(WebSocketDestination.getChatDestination(response.roomId()), response);
    }

    public void publishReadMessageAfterCommit(Long roomId, Long currentUserId, Long lastReadMessageId) {
        webSocketPublisher.sendAfterCommit(
                WebSocketDestination.getChatReadDestination(roomId),
                ReadMessageEvent.from(roomId, currentUserId, lastReadMessageId)
        );
    }

    public void publishDeleteMessageAfterCommit(Long roomId, Long messageId) {
        webSocketPublisher.sendAfterCommit(
                WebSocketDestination.getChatDeleteDestination(roomId),
                new ChatMessageDeletedEvent(
                        roomId,
                        messageId,
                        CommonConst.DELETED_MESSAGE
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
                            WebSocketDestination.getChatDestination(response.roomId()),
                            response
                    )
            );
        });
    }
}
