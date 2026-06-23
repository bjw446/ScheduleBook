package com.example.schedulebook.domain.chat.repository;

import com.example.schedulebook.domain.chat.entity.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    Slice<ChatMessage> findByChatRoomIdOrderByIdDesc(Long roomId, Pageable pageable);
}
