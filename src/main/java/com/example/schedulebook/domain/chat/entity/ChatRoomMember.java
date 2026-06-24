package com.example.schedulebook.domain.chat.entity;

import com.example.schedulebook.common.entity.DeleteEntity;
import com.example.schedulebook.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "chat_room_members",
        uniqueConstraints = {
                @UniqueConstraint(
                        columnNames = {"chat_room_id", "user_id"}
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoomMember extends DeleteEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom chatRoom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "last_read_message_id")
    private Long lastReadMessageId;

    @Column(nullable = false, name = "unread_count")
    private int unreadCount;

    @Column(nullable = false, name = "joined_at")
    private LocalDateTime joinedAt;

    public static ChatRoomMember of(ChatRoom chatRoom, User user, LocalDateTime joinedAt) {
        ChatRoomMember chatRoomMember = new ChatRoomMember();

        chatRoomMember.user = user;
        chatRoomMember.joinedAt = joinedAt;
        chatRoomMember.unreadCount = 0;

        chatRoom.addMember(chatRoomMember);

        return chatRoomMember;
    }

    public void assignChatRoom(ChatRoom chatRoom) {
        this.chatRoom = chatRoom;
    }

    public void updateLastRead(Long lastReadMessageId) {
        if (lastReadMessageId == null) {
            return;
        }

        if (this.lastReadMessageId == null || lastReadMessageId > this.lastReadMessageId) {
            this.lastReadMessageId = lastReadMessageId;
        }
    }

    public void clearUnreadCount() {
        this.unreadCount = 0;
    }

    public void leaveChatRoom() {
        delete();
    }

    public void rejoin() {
        restore();
        unreadCount = 0;
        lastReadMessageId = null;
        joinedAt = LocalDateTime.now();
    }
}
