package com.example.schedulebook.domain.chatroom.repository;

import com.example.schedulebook.domain.chatroom.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE ChatRoom c SET c.memberCount = c.memberCount - 1 WHERE c.id = :roomId AND c.memberCount > 0")
    int decreaseMemberCount(@Param("roomId") Long roomId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE ChatRoom c SET c.memberCount = c.memberCount + :count WHERE c.id = :roomId")
    int increaseMemberCount(@Param("roomId") Long roomId, @Param("count") int count);
}
