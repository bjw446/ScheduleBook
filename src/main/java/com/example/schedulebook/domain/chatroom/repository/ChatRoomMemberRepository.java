package com.example.schedulebook.domain.chatroom.repository;

import com.example.schedulebook.domain.chatroom.entity.ChatRoomMember;
import com.example.schedulebook.domain.chatroom.projection.ChatRoomListProjection;
import com.example.schedulebook.domain.chatroom.projection.MemberReadStatusProjection;
import com.example.schedulebook.domain.chatroom.projection.OpponentInfoProjection;
import com.example.schedulebook.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatRoomMemberRepository extends JpaRepository<ChatRoomMember, Long> {
    @Query("SELECT crm.user FROM ChatRoomMember crm WHERE crm.chatRoom.id = :roomId " +
            "AND crm.user.id = :userId AND crm.deletedAt IS NULL")
    Optional<User> findUserInRoom(@Param("roomId") Long roomId, @Param("userId") Long userId);

    @Query("SELECT crm FROM ChatRoomMember crm WHERE crm.chatRoom.id = :roomId " +
            "AND crm.user.id = :userId AND crm.deletedAt IS NULL")
    Optional<ChatRoomMember> findActiveByChatRoomIdAndUserId(@Param("roomId") Long roomId, @Param("userId") Long userId);

    @Modifying
    @Query("UPDATE ChatRoomMember crm SET crm.unreadCount = crm.unreadCount + 1 " +
            "WHERE crm.chatRoom.id = :roomId AND crm.user.id <> :senderId AND crm.deletedAt IS NULL")
    void increaseUnreadCount(@Param("roomId") Long roomId, @Param("senderId") Long senderId);

    @Query("SELECT crm.chatRoom.id as roomId, crm.chatRoom.name as roomName, " +
            "CASE WHEN crm.chatRoom.chatRoomType = com.example.schedulebook.domain.chatroom.enums.ChatRoomType.DIRECT " +
            "THEN (SELECT CASE WHEN opponent.user.deletedAt IS NOT NULL THEN '알 수 없음' " +
            "ELSE opponent.user.nickname END FROM ChatRoomMember opponent " +
            "WHERE opponent.chatRoom.id = crm.chatRoom.id AND opponent.user.id <> :userId) " +
            "ELSE NULL END as opponentNickname, lm.content as lastMessage, lm.createdAt " +
            "as lastMessageAt, crm.unreadCount as unreadCount, crm.chatRoom.chatRoomType " +
            "as chatRoomType FROM ChatRoomMember crm LEFT JOIN crm.chatRoom.lastMessage lm " +
            "WHERE crm.user.id = :userId AND crm.deletedAt IS NULL " +
            "ORDER BY CASE WHEN lm.createdAt IS NULL THEN 1 ELSE 0 END, lm.createdAt DESC")
    List<ChatRoomListProjection> findMyChatRooms(@Param("userId") Long userId);

    @Query("SELECT crm.user.nickname as nickname, crm.user.deletedAt as userDeletedAt " +
            "FROM ChatRoomMember crm WHERE crm.chatRoom.id = :roomId AND crm.user.id <> :currentUserId")
    OpponentInfoProjection findOpponentInfo(@Param("roomId") Long roomId, @Param("currentUserId") Long currentUserId);

    @Query("SELECT crm FROM ChatRoomMember crm WHERE crm.chatRoom.id = :roomId " +
            "AND crm.user.id <> :senderId AND crm.deletedAt IS NOT NULL")
    List<ChatRoomMember> findDeletedMembers(@Param("roomId") Long roomId, @Param("senderId") Long senderId);

    @Query("SELECT crm.user.id as userId, crm.user.nickname as nickname, " +
            "crm.lastReadMessageId as lastReadMessageId, crm.joinedAt as joinedAt FROM ChatRoomMember crm " +
            "WHERE crm.chatRoom.id = :roomId AND crm.deletedAt IS NULL AND crm.user.deletedAt IS NULL")
    List<MemberReadStatusProjection> findReadStatuses(@Param("roomId") Long roomId);

    @Query("SELECT crm FROM ChatRoomMember crm WHERE crm.chatRoom.id = :roomId AND crm.user.id = :userId")
    Optional<ChatRoomMember> findByChatRoomIdAndUserId(@Param("roomId") Long roomId, @Param("userId") Long userId);

    @Modifying
    @Query("UPDATE ChatRoomMember crm SET crm.unreadCount = crm.unreadCount + 1 " +
            "WHERE crm.chatRoom.id = :roomId AND crm.deletedAt IS NULL")
    void increaseUnreadCountAll(Long roomId);

    @Query("SELECT crm FROM ChatRoomMember crm JOIN FETCH crm.chatRoom " +
            "WHERE crm.user.id = :userId AND crm.deletedAt IS NULL")
    List<ChatRoomMember> findAllByUserId(@Param("userId") Long userId);
}
