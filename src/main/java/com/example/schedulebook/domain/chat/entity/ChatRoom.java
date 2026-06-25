package com.example.schedulebook.domain.chat.entity;

import com.example.schedulebook.common.entity.DeleteEntity;
import com.example.schedulebook.domain.chat.enums.ChatRoomType;
import com.fasterxml.jackson.annotation.JsonIgnore;
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

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_message_id")
    private ChatMessage lastMessage;

    @Column(nullable = false, name = "member_count")
    private int memberCount;

    public static ChatRoom direct() {
        ChatRoom chatRoom = new ChatRoom();

        chatRoom.chatRoomType = ChatRoomType.DIRECT;
        chatRoom.memberCount = 0;

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

    public void addMember(ChatRoomMember chatRoomMember) {
        this.memberCount++;
        chatRoomMember.assignChatRoom(this);
    }

    public void decreaseMemberCount() {
        if (this.memberCount > 0) {
            this.memberCount--;
        }
    }

    public void increaseMemberCount() {
        this.memberCount++;
    }
}
