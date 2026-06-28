package com.example.schedulebook.domain.chat.event;

import com.example.schedulebook.domain.chat.dto.request.PublishChatMessage;
import com.example.schedulebook.domain.chat.dto.response.ChatMessageResponse;
import com.example.schedulebook.domain.chat.entity.ChatMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

import static com.example.schedulebook.domain.chat.consts.ChatConst.DELETE_MESSAGE;

@Component
@RequiredArgsConstructor
public class ChatMessagePublisher {
    private final SimpMessagingTemplate simpMessagingTemplate;

    public void publishMessage(ChatMessage chatMessage, int unreadCount) {
        ChatMessageResponse response = ChatMessageResponse.from(chatMessage, unreadCount);

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

    public void publishReadMessageAfterCommit(Long roomId, Long currentUserId, Long lastReadMessageId) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                simpMessagingTemplate.convertAndSend(
                        "/topic/chat/" + roomId + "/read",
                        ReadMessageEvent.from(roomId, currentUserId, lastReadMessageId)
                );
            }
        });
    }

    public void publishDeleteMessageAfterCommit(Long roomId, Long messageId) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                simpMessagingTemplate.convertAndSend(
                        "/topic/chat/" + roomId + "/delete",
                        new ChatMessageDeletedEvent(
                                roomId,
                                messageId,
                                DELETE_MESSAGE
                        )
                );
            }
        });
    }

    public void publishMessages(List<PublishChatMessage> publishChatMessages) {
        List<ChatMessageResponse> responses =
                publishChatMessages.stream()
                        .map(item -> ChatMessageResponse.from(item.chatMessage(), item.unreadCount()))
                        .toList();

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {

                        for (int i = 0; i < responses.size(); i++) {
                            ChatMessage message = publishChatMessages.get(i).chatMessage();

                            simpMessagingTemplate.convertAndSend(
                                    "/topic/chat/" + message.getChatRoom().getId(),
                                    responses.get(i)
                            );
                        }
                    }
                });
    }
}
