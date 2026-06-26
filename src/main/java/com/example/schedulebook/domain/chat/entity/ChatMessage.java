package com.example.schedulebook.domain.chat.entity;

import com.example.schedulebook.common.entity.ModifyEntity;
import com.example.schedulebook.common.enums.ErrorEnum;
import com.example.schedulebook.common.exception.BaseException;
import com.example.schedulebook.domain.chat.enums.ChatMessageType;
import com.example.schedulebook.domain.schedule.entity.Schedule;
import com.example.schedulebook.domain.schedule.entity.ScheduleSnapshot;
import com.example.schedulebook.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import static com.example.schedulebook.domain.chat.consts.ChatConst.DELETE_MESSAGE;

@Getter
@Entity
@Table(name = "chat_messages")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessage extends ModifyEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom chatRoom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id")
    private User sender;

    @Column(name = "schedule_id")
    private Long scheduleId;

    @Embedded
    private ScheduleSnapshot scheduleSnapshot;

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

    @Column(nullable = false)
    private boolean deleted;

    @Column(name = "schedule_share_canceled")
    private boolean scheduleShareCanceled;

    private ChatMessage(ChatRoom chatRoom, User sender, String content, ChatMessageType chatMessageType, ChatMessage replyMessage) {
        this.chatRoom = chatRoom;
        this.sender = sender;
        this.content = content;
        this.chatMessageType = chatMessageType;
        this.replyMessage = replyMessage;
        this.edited = false;
        this.deleted = false;
    }

    public static ChatMessage of(ChatRoom chatRoom, User sender, String content, ChatMessageType chatMessageType, ChatMessage replyMessage) {
        return new ChatMessage(chatRoom, sender, content, chatMessageType, replyMessage);
    }

    public static ChatMessage schedule(ChatRoom chatRoom, User sender, Schedule schedule) {
        ChatMessage chatMessage = new ChatMessage();

        chatMessage.chatRoom = chatRoom;
        chatMessage.sender = sender;

        chatMessage.scheduleId = schedule.getId();
        chatMessage.scheduleSnapshot = ScheduleSnapshot.from(schedule);
        chatMessage.chatMessageType = ChatMessageType.SCHEDULE;

        chatMessage.deleted = false;
        chatMessage.scheduleShareCanceled = false;

        return chatMessage;
    }

    public void deleteMessage(Long userId) {
        if (sender == null) {
            throw new BaseException(ErrorEnum.CHAT_MESSAGE_DELETE_NOT_ALLOWED);
        }

        if (!sender.getId().equals(userId)) {
            throw new BaseException(ErrorEnum.CHAT_MESSAGE_FORBIDDEN);
        }

        this.deleted = true;
        this.content = DELETE_MESSAGE;
    }

    public void cancelScheduleShare(Long userId) {
        if (!sender.getId().equals(userId)) {
            throw new BaseException(ErrorEnum.CHAT_MESSAGE_FORBIDDEN);
        }

        this.scheduleShareCanceled = true;
    }

    public void cancelScheduleShare() {
        this.scheduleShareCanceled = true;
    }

    public void updateScheduleSnapshot(Schedule schedule) {
        if (this.scheduleSnapshot.getScheduleVersion() >= schedule.getScheduleVersion()) {
            return;
        }
        this.scheduleSnapshot = ScheduleSnapshot.from(schedule);
    }
}
