package com.example.schedulebook.domain.chatroom.repository;

import com.example.schedulebook.domain.chatroom.entity.ChatRoomMember;
import com.example.schedulebook.domain.chatroom.projection.ChatRoomListProjection;
import com.example.schedulebook.domain.chatroom.projection.MemberReadStatusProjection;
import com.example.schedulebook.domain.chatroom.projection.OpponentInfoProjection;
import com.example.schedulebook.domain.user.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
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

    @Query("SELECT crm.chatRoom.id as roomId, crm.chatRoom.name as roomName, " +
            "CASE WHEN crm.chatRoom.chatRoomType = com.example.schedulebook.domain.chatroom.enums.ChatRoomType.DIRECT " +
            "THEN ( SELECT CASE WHEN opponent.user.deletedAt IS NOT NULL THEN '알 수 없음' " +
            "ELSE opponent.user.nickname END FROM ChatRoomMember opponent " +
            "WHERE opponent.chatRoom.id = crm.chatRoom.id AND opponent.user.id <> :currentUserId) " +
            "ELSE NULL END as opponentNickname, lm.content as lastMessage, lm.createdAt as lastMessageAt, " +
            "crm.unreadCount as unreadCount, crm.chatRoom.chatRoomType as chatRoomType " +
            "FROM ChatRoomMember crm LEFT JOIN crm.chatRoom.lastMessage lm WHERE crm.user.id = :currentUserId " +
            "AND crm.deletedAt IS NULL AND ((:cursorTime IS NULL AND :cursorRoomId IS NULL) " +
            "OR (:cursorTime IS NOT NULL AND ( lm.createdAt < :cursorTime OR ( lm.createdAt = :cursorTime " +
            "AND crm.chatRoom.id < :cursorRoomId) OR lm.createdAt IS NULL)) " +
            "OR (:cursorTime IS NULL AND :cursorRoomId IS NOT NULL AND lm.createdAt IS NULL " +
            "AND crm.chatRoom.id < :cursorRoomId)) ORDER BY CASE WHEN lm.createdAt IS NULL " +
            "THEN 1 ELSE 0 END, lm.createdAt DESC, crm.chatRoom.id DESC")
    List<ChatRoomListProjection> findMyChatRooms(@Param("currentUserId") Long currentUserId,
                                                 @Param("cursorTime") LocalDateTime cursorTime,
                                                 @Param("cursorRoomId") Long cursorRoomId,
                                                 Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE ChatRoomMember crm SET crm.deletedAt = :deletedAt WHERE crm.id = :memberId AND crm.deletedAt IS NULL")
    int leave(@Param("memberId") Long memberId, @Param("deletedAt") LocalDateTime deletedAt);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE ChatRoomMember crm SET crm.deletedAt = NULL, crm.unreadCount = 0, crm.lastReadMessageId = NULL, " +
            "crm.joinedAt = :joinedAt WHERE crm.id = :memberId AND crm.deletedAt IS NOT NULL")
    int rejoin(@Param("memberId") Long memberId, @Param("joinedAt") LocalDateTime joinedAt);
}