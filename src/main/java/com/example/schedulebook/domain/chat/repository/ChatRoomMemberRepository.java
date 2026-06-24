package com.example.schedulebook.domain.chat.repository;

import com.example.schedulebook.domain.chat.entity.ChatRoomMember;
import com.example.schedulebook.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatRoomMemberRepository extends JpaRepository<ChatRoomMember, Long> {
    List<ChatRoomMember> findAllByUserId(Long userId);

    List<ChatRoomMember> findAllByChatRoomId(Long roomId);

    @Query("SELECT crm.user FROM ChatRoomMember crm WHERE crm.chatRoom.id = :roomId AND crm.user.id = :userId")
    Optional<User> findUserInRoom(@Param("roomId") Long roomId, @Param("userId") Long userId);

    Optional<ChatRoomMember> findByChatRoomIdAndUserId(Long roomId, Long userId);
}
