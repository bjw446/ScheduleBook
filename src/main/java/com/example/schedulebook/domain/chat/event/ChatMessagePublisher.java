package com.example.schedulebook.domain.chat.event;

import com.example.schedulebook.common.executor.AfterCommitExecutor;
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

    public void publishMessage(ChatMessage chatMessage, int unreadCount) {
        ChatMessageResponse response = ChatMessageResponse.from(chatMessage, unreadCount);

        afterCommitExecutor.execute(() -> {
            try {
                simpMessagingTemplate.convertAndSend(
                        "/topic/chat/" + chatMessage.getChatRoom().getId(),
                        response
                );
            } catch (Exception e) {
                log.error("채팅 메시지 발행 실패", e);
            }
        });
    }

    public void publishReadMessageAfterCommit(Long roomId, Long currentUserId, Long lastReadMessageId) {
        afterCommitExecutor.execute(() -> {
            try {
                simpMessagingTemplate.convertAndSend(
                        "/topic/chat/" + roomId + "/read",
                        ReadMessageEvent.from(roomId, currentUserId, lastReadMessageId)
                );
            } catch (Exception e) {
                log.error("채팅 메시지 읽음 이벤트 발행 실패", e);
            }
        });
    }

    public void publishDeleteMessageAfterCommit(Long roomId, Long messageId) {
        afterCommitExecutor.execute(() -> {
            try {
                simpMessagingTemplate.convertAndSend(
                        "/topic/chat/" + roomId + "/delete",
                        new ChatMessageDeletedEvent(
                                roomId,
                                messageId,
                                DELETED_MESSAGE
                        )
                );
            } catch (Exception e) {
                log.error("채팅 메시지 삭제 이벤트 발행 실패", e);
            }
        });
    }

    public void publishMessages(List<PublishChatMessage> publishChatMessages) {
        List<ChatMessageResponse> responses =
                publishChatMessages.stream()
                        .map(item -> ChatMessageResponse.from(item.chatMessage(), item.unreadCount()))
                        .toList();

        afterCommitExecutor.execute(() -> {
            for (int i = 0; i < responses.size(); i++) {
                 try {
                    ChatMessage message = publishChatMessages.get(i).chatMessage();

                    simpMessagingTemplate.convertAndSend(
                            "/topic/chat/" + message.getChatRoom().getId(),
                            responses.get(i)
                    );
                } catch (Exception e) {
                     log.error("채팅 메시지 발행 실패", e);
                 }
            }
        });
    }
}
