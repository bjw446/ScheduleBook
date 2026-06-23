package com.example.schedulebook.domain.chat.entity;

import com.example.schedulebook.common.entity.DeleteEntity;
import com.example.schedulebook.domain.chat.enums.ChatRoomType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "chat_rooms")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoom extends DeleteEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChatRoomType chatRoomType;

    @Column(length = 100)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_message_id")
    private ChatMessage lastMessage;

    @Column(nullable = false, name = "member_count")
    private Integer memberCount;

    private ChatRoom(ChatRoomType chatRoomType, String name, Integer memberCount) {
        this.chatRoomType = chatRoomType;
        this.name = name;
        this.memberCount = memberCount;
    }

    public static ChatRoom direct() {
        ChatRoom chatRoom = new ChatRoom();

        chatRoom.chatRoomType = ChatRoomType.DIRECT;
        chatRoom.memberCount = 2;

        return chatRoom;
    }

    public static ChatRoom group(String name) {
        ChatRoom chatRoom = new ChatRoom();

        chatRoom.name = name;
        chatRoom.chatRoomType = ChatRoomType.GROUP;
        chatRoom.memberCount = 0;

        return chatRoom;
    }

    public void updateLastMessage(ChatMessage lastMessage) {
        this.lastMessage = lastMessage;
    }

    public void increaseMemberCount() {
        this.memberCount++;
    }

    public void decreaseMemberCount() {
        if (this.memberCount > 0) {
            this.memberCount--;
        }
    }
}
