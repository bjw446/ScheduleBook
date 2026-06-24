package com.example.schedulebook.domain.chat.repository;

import com.example.schedulebook.domain.chat.entity.ChatRoomMember;
import com.example.schedulebook.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatRoomMemberRepository extends JpaRepository<ChatRoomMember, Long> {
    @Query("SELECT crm.user FROM ChatRoomMember crm WHERE crm.chatRoom.id = :roomId AND crm.user.id = :userId")
    Optional<User> findUserInRoom(@Param("roomId") Long roomId, @Param("userId") Long userId);

    Optional<ChatRoomMember> findByChatRoomIdAndUserId(Long roomId, Long userId);

    @Modifying
    @Query("UPDATE ChatRoomMember crm SET crm.unreadCount = crm.unreadCount + 1 " +
            "WHERE crm.chatRoom.id = :roomId AND crm.user.id <> :senderId AND crm.deletedAt IS NULL")
    void increaseUnreadCount(@Param("roomId") Long roomId, @Param("senderId") Long senderId);

    @Query("SELECT crm.chatRoom.id as roomId, crm.chatRoom.name as roomName, " +
            "CASE WHEN crm.chatRoom.chatRoomType = com.example.schedulebook.domain.chat.enums.ChatRoomType.DIRECT " +
            "THEN (SELECT opponent.user.nickname FROM ChatRoomMember opponent " +
            "WHERE opponent.chatRoom.id = crm.chatRoom.id AND opponent.user.id <> :userId " +
            "AND opponent.deletedAt IS NULL) ELSE NULL END as opponentNickname, " +
            "lm.content as lastMessage, lm.createdAt as lastMessageAt, " +
            "crm.unreadCount as unreadCount, crm.chatRoom.chatRoomType as chatRoomType " +
            "FROM ChatRoomMember crm LEFT JOIN crm.chatRoom.lastMessage lm " +
            "WHERE crm.user.id = :userId AND crm.deletedAt IS NULL " +
            "ORDER BY CASE WHEN lm.createdAt IS NULL THEN 1 ELSE 0 END, lm.createdAt DESC")
    List<ChatRoomListProjection> findMyChatRooms(@Param("userId") Long userId);
}
