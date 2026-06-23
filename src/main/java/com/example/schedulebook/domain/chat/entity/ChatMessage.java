package com.example.schedulebook.domain.chat.entity;

import com.example.schedulebook.common.entity.DeleteEntity;
import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.domain.chat.enums.ChatMessageType;
import com.example.schedulebook.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "chat_messages")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessage extends DeleteEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id")
    private ChatRoom chatRoom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id")
    private User sender;

    @Column(length = 1000)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChatMessageType chatMessageType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reply_message_id")
    private ChatMessage replyMessage;

    @Column(nullable = false)
    private boolean edited;

    @Column(name = "edited_at")
    private LocalDateTime editedAt;

    private ChatMessage(ChatRoom chatRoom, User sender, String content, ChatMessageType chatMessageType, ChatMessage replyMessage) {
        this.chatRoom = chatRoom;
        this.sender = sender;
        this.content = content;
        this.chatMessageType = chatMessageType;
        this.replyMessage = replyMessage;
        this.edited = false;
    }

    public static ChatMessage of(ChatRoom chatRoom, User sender, String content, ChatMessageType chatMessageType, ChatMessage replyMessage) {
        return new ChatMessage(chatRoom, sender, content, chatMessageType, replyMessage);
    }

    public void updateMessage(String content) {
        if (chatMessageType != ChatMessageType.TEXT) {
            throw new BaseException(ErrorEnum.INVALID_MESSAGE_TYPE);
        }

        this.content = content;
        this.edited = true;
        this.editedAt = LocalDateTime.now();
    }
}
