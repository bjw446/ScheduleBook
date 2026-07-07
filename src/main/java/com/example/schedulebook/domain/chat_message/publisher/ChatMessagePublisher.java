package com.example.schedulebook.domain.chat_message.publisher;

import com.example.schedulebook.common.executor.AfterCommitExecutor;
import com.example.schedulebook.common.websocket.WebSocketPublisher;
import com.example.schedulebook.domain.chat_message.dto.request.PublishChatMessage;
import com.example.schedulebook.domain.chat_message.dto.response.ChatMessageResponse;
import com.example.schedulebook.domain.chat_message.entity.ChatMessage;
import com.example.schedulebook.domain.chat_message.event.ChatMessageDeletedEvent;
import com.example.schedulebook.domain.chat_message.event.ReadMessageEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.example.schedulebook.common.consts.CommonConst.DELETED_MESSAGE;
import static com.example.schedulebook.common.consts.WebSocketDestination.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatMessagePublisher {
    private final AfterCommitExecutor afterCommitExecutor;
    private final WebSocketPublisher webSocketPublisher;

    public void publishMessage(ChatMessage chatMessage, int unreadCount) {
        ChatMessageResponse response = ChatMessageResponse.from(chatMessage, unreadCount);

        webSocketPublisher.sendAfterCommit(chat(response.roomId()), response);
    }

    public void publishReadMessageAfterCommit(Long roomId, Long currentUserId, Long lastReadMessageId) {
        webSocketPublisher.sendAfterCommit(
                chatRead(roomId),
                ReadMessageEvent.from(roomId, currentUserId, lastReadMessageId)
        );
    }

    public void publishDeleteMessageAfterCommit(Long roomId, Long messageId) {
        webSocketPublisher.sendAfterCommit(
                chatDelete(roomId),
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
                            chat(response.roomId()),
                            response
                    )
            );
        });
    }
}
