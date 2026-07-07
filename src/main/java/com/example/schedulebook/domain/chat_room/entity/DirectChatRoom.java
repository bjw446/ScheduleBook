package com.example.schedulebook.domain.chat_room.entity;

import com.example.schedulebook.common.entity.DeleteEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "direct_chat_rooms",
        uniqueConstraints = {
                @UniqueConstraint(
                        columnNames = {
                                "user1_id",
                                "user2_id"
                        }
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DirectChatRoom extends DeleteEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user1_id", nullable = false)
    private Long user1Id;

    @Column(name = "user2_id", nullable = false)
    private Long user2Id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom chatRoom;

    private DirectChatRoom(Long user1Id, Long user2Id, ChatRoom chatRoom) {
        this.user1Id = user1Id;
        this.user2Id = user2Id;
        this.chatRoom = chatRoom;
    }

    public static DirectChatRoom of(Long user1Id, Long user2Id, ChatRoom chatRoom) {
        long min = Math.min(user1Id, user2Id);

        long max = Math.max(user1Id, user2Id);

        return new DirectChatRoom(min, max, chatRoom);
    }
}
