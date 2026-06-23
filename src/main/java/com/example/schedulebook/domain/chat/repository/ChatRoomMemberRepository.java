package com.example.schedulebook.domain.chat.repository;

import com.example.schedulebook.domain.chat.entity.ChatRoomMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatRoomMemberRepository extends JpaRepository<ChatRoomMember, Long> {
    List<ChatRoomMember> findAllByUserId(Long userId);

    List<ChatRoomMember> findAllByChatRoomId(Long roomId);
}
