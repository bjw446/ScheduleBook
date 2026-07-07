package com.example.schedulebook.domain.chatroom.repository;

import com.example.schedulebook.domain.chatroom.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
}
