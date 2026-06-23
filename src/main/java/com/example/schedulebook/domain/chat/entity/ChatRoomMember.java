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
    @JoinColumn(name = "chat_room_id")
    private ChatRoom chatRoom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "last_read_message_id")
    private Long lastReadMessageId;

    @Column(nullable = false, name = "joined_at")
    private LocalDateTime joinedAt;

    private ChatRoomMember(ChatRoom chatRoom, User user, LocalDateTime joinedAt) {
        this.chatRoom = chatRoom;
        this.user = user;
        this.joinedAt = joinedAt;
    }

    public static ChatRoomMember of(ChatRoom chatRoom, User user, LocalDateTime joinedAt) {
        return new ChatRoomMember(chatRoom, user, joinedAt);
    }

    public void updateLastRead(Long lastReadMessageId) {
        this.lastReadMessageId = lastReadMessageId;
    }
}
