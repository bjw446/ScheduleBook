package com.example.schedulebook.domain.chatroom.repository;

import com.example.schedulebook.domain.chatroom.entity.DirectChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DirectChatRoomRepository extends JpaRepository<DirectChatRoom, Long> {
    Optional<DirectChatRoom> findByUser1IdAndUser2Id(Long user1Id, Long user2Id);
}
