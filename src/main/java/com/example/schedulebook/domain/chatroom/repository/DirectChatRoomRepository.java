package com.example.schedulebook.domain.chatroom.repository;

import com.example.schedulebook.domain.chatroom.entity.DirectChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface DirectChatRoomRepository extends JpaRepository<DirectChatRoom, Long> {
    Optional<DirectChatRoom> findByUser1IdAndUser2Id(Long user1Id, Long user2Id);

    @Modifying
    @Query("DELETE FROM DirectChatRoom d WHERE d.user1Id = :userId OR d.user2Id = :userId")
    void deleteDirectChatRoomByUserId(@Param("userId") Long userId);
}
