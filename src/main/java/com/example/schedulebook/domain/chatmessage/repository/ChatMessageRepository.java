package com.example.schedulebook.domain.chatmessage.repository;

import com.example.schedulebook.domain.chatmessage.entity.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    Optional<ChatMessage> findByIdAndChatRoomId(Long messageId, Long roomId);

    @Query("SELECT cm FROM ChatMessage cm JOIN FETCH cm.sender " +
            "LEFT JOIN FETCH cm.replyMessage rm LEFT JOIN FETCH rm.sender " +
            "WHERE cm.chatRoom.id = :roomId AND cm.createdAt >= :joinedAt " +
            "AND (:cursor IS NULL OR cm.id < :cursor) ORDER BY cm.id DESC")
    List<ChatMessage> findMessages(
            @Param("roomId") Long roomId,
            @Param("joinedAt") LocalDateTime joinedAt,
            @Param("cursor") Long cursor,
            Pageable pageable
    );

    @Query("SELECT COUNT(cm) FROM ChatMessage cm WHERE cm.chatRoom.id = :roomId " +
            "AND cm.createdAt >= :joinedAt and cm.id > :lastReadMessageId " +
            "AND (cm.sender IS NULL OR cm.sender.id <> :userId) ")
    long countUnreadMessages(
            @Param("roomId") Long roomId,
            @Param("lastReadMessageId") Long lastReadMessageId,
            @Param("userId") Long userId,
            @Param("joinedAt") LocalDateTime joinedAt
    );

    List<ChatMessage> findAllByScheduleIdAndDeletedFalse(Long scheduleId);

    @Query("SELECT cm FROM ChatMessage cm JOIN FETCH cm.chatRoom LEFT JOIN FETCH cm.sender WHERE cm.id = :messageId")
    Optional<ChatMessage> findByIdWithChatRoom(@Param("messageId") Long messageId);
}