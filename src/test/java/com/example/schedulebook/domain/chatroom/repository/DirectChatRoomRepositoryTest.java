package com.example.schedulebook.domain.chatroom.repository;

import com.example.schedulebook.common.config.JpaAuditingConfig;
import com.example.schedulebook.domain.chatroom.entity.ChatRoom;
import com.example.schedulebook.domain.chatroom.entity.DirectChatRoom;
import jakarta.persistence.EntityManager;
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
class DirectChatRoomRepositoryTest {

    @Autowired
    DirectChatRoomRepository directChatRoomRepository;

    @Autowired
    ChatRoomRepository chatRoomRepository;

    @Autowired
    EntityManager entityManager;

    private DirectChatRoom createDirectChatRoom(Long user1Id, Long user2Id) {
        ChatRoom chatRoom = chatRoomRepository.save(ChatRoom.direct());

        return directChatRoomRepository.save(
                DirectChatRoom.of(user1Id, user2Id, chatRoom)
        );
    }

    @Test
    @DisplayName("두 사용자 ID로 1:1 채팅방을 조회한다")
    void findByUser1IdAndUser2Id() {
        // given
        DirectChatRoom directChatRoom = createDirectChatRoom(1L, 2L);

        // when
        DirectChatRoom foundDirectChatRoom = directChatRoomRepository
                .findByUser1IdAndUser2Id(1L, 2L)
                .orElseThrow();

        // then
        assertThat(foundDirectChatRoom.getId()).isEqualTo(directChatRoom.getId());
        assertThat(foundDirectChatRoom.getUser1Id()).isEqualTo(1L);
        assertThat(foundDirectChatRoom.getUser2Id()).isEqualTo(2L);
    }

    @Test
    @DisplayName("존재하지 않는 사용자 조합으로 조회하면 빈 Optional을 반환한다")
    void findByUser1IdAndUser2IdWhenNotExists() {
        // given
        createDirectChatRoom(1L, 2L);

        // when
        var result = directChatRoomRepository.findByUser1IdAndUser2Id(3L, 4L);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("사용자 ID를 역순으로 전달해도 정렬된 ID로 1:1 채팅방을 조회한다")
    void findByUser1IdAndUser2IdWhenCreatedInReverseOrder() {
        // given
        DirectChatRoom directChatRoom = createDirectChatRoom(2L, 1L);

        // when
        DirectChatRoom foundDirectChatRoom = directChatRoomRepository
                .findByUser1IdAndUser2Id(1L, 2L)
                .orElseThrow();

        // then
        assertThat(foundDirectChatRoom.getId()).isEqualTo(directChatRoom.getId());
        assertThat(foundDirectChatRoom.getUser1Id()).isEqualTo(1L);
        assertThat(foundDirectChatRoom.getUser2Id()).isEqualTo(2L);
    }

    @Test
    @DisplayName("첫 번째 사용자 ID에 해당하는 1:1 채팅방을 삭제한다")
    void deleteDirectChatRoomByUserIdWhenUser1() {
        // given
        DirectChatRoom target = createDirectChatRoom(1L, 2L);
        DirectChatRoom unrelated = createDirectChatRoom(3L, 4L);

        // when
        directChatRoomRepository.deleteDirectChatRoomByUserId(1L);
        entityManager.clear();

        // then
        assertThat(directChatRoomRepository.findById(target.getId())).isEmpty();
        assertThat(directChatRoomRepository.findById(unrelated.getId())).isPresent();
    }

    @Test
    @DisplayName("두 번째 사용자 ID에 해당하는 1:1 채팅방을 삭제한다")
    void deleteDirectChatRoomByUserIdWhenUser2() {
        // given
        DirectChatRoom target = createDirectChatRoom(1L, 2L);
        DirectChatRoom anotherTarget = createDirectChatRoom(3L, 2L);
        DirectChatRoom unrelated = createDirectChatRoom(3L, 4L);

        // when
        directChatRoomRepository.deleteDirectChatRoomByUserId(2L);
        entityManager.clear();

        // then
        assertThat(directChatRoomRepository.findById(target.getId())).isEmpty();
        assertThat(directChatRoomRepository.findById(anotherTarget.getId())).isEmpty();
        assertThat(directChatRoomRepository.findById(unrelated.getId())).isPresent();
    }

    @Test
    @DisplayName("해당 사용자가 포함되지 않은 1:1 채팅방은 삭제하지 않는다")
    void deleteDirectChatRoomByUserIdWhenNotExists() {
        // given
        DirectChatRoom directChatRoom = createDirectChatRoom(1L, 2L);

        // when
        directChatRoomRepository.deleteDirectChatRoomByUserId(3L);
        entityManager.clear();

        // then
        assertThat(directChatRoomRepository.findById(directChatRoom.getId())).isPresent();
    }
}