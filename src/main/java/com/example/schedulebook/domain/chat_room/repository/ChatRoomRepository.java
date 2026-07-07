package com.example.schedulebook.domain.chat_room.repository;

import com.example.schedulebook.domain.chat_room.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
}
