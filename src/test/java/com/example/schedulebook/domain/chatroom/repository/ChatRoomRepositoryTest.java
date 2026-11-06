package com.example.schedulebook.domain.chatroom.repository;

import com.example.schedulebook.common.config.JpaAuditingConfig;
import com.example.schedulebook.domain.chatroom.entity.ChatRoom;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaAuditingConfig.class)
class ChatRoomRepositoryTest {

    @Autowired
    ChatRoomRepository chatRoomRepository;

    @Test
    @DisplayName("채팅방 멤버 수를 1 감소시킨다")
    void decreaseMemberCount() {
        // given
        ChatRoom chatRoom = ChatRoom.group("테스트 그룹 채팅방");
        chatRoomRepository.save(chatRoom);

        chatRoomRepository.increaseMemberCount(chatRoom.getId(), 3);

        // when
        int result = chatRoomRepository.decreaseMemberCount(chatRoom.getId());

        // then
        ChatRoom foundChatRoom = chatRoomRepository.findById(chatRoom.getId())
                .orElseThrow();

        assertThat(result).isEqualTo(1);
        assertThat(foundChatRoom.getMemberCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("채팅방 멤버 수가 0이면 감소시키지 않는다")
    void decreaseMemberCountWhenZero() {
        // given
        ChatRoom chatRoom = ChatRoom.group("테스트 그룹 채팅방");
        chatRoomRepository.save(chatRoom);

        // when
        int result = chatRoomRepository.decreaseMemberCount(chatRoom.getId());

        // then
        ChatRoom foundChatRoom = chatRoomRepository.findById(chatRoom.getId())
                .orElseThrow();

        assertThat(result).isZero();
        assertThat(foundChatRoom.getMemberCount()).isZero();
    }

    @Test
    @DisplayName("존재하지 않는 채팅방의 멤버 수를 감소시키면 변경 건수 0을 반환한다")
    void decreaseMemberCountWhenRoomNotExists() {
        // given
        Long roomId = Long.MAX_VALUE;

        // when
        int result = chatRoomRepository.decreaseMemberCount(roomId);

        // then
        assertThat(result).isZero();
    }

    @Test
    @DisplayName("채팅방 멤버 수를 지정한 수만큼 증가시킨다")
    void increaseMemberCount() {
        // given
        ChatRoom chatRoom = ChatRoom.group("테스트 그룹 채팅방");
        chatRoomRepository.save(chatRoom);

        // when
        int result = chatRoomRepository.increaseMemberCount(chatRoom.getId(), 3);

        // then
        ChatRoom foundChatRoom = chatRoomRepository.findById(chatRoom.getId())
                .orElseThrow();

        assertThat(result).isEqualTo(1);
        assertThat(foundChatRoom.getMemberCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("존재하지 않는 채팅방의 멤버 수를 증가시키면 변경 건수 0을 반환한다")
    void increaseMemberCountWhenRoomNotExists() {
        // given
        Long roomId = Long.MAX_VALUE;

        // when
        int result = chatRoomRepository.increaseMemberCount(roomId, 3);

        // then
        assertThat(result).isZero();
    }
}