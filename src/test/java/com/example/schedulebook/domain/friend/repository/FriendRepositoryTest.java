package com.example.schedulebook.domain.friend.repository;

import com.example.schedulebook.common.config.JpaAuditingConfig;
import com.example.schedulebook.domain.friend.entity.Friend;
import com.example.schedulebook.domain.friend.enums.FriendStatus;
import com.example.schedulebook.domain.user.entity.User;
import jakarta.persistence.EntityManager;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaAuditingConfig.class)
class FriendRepositoryTest {

    @Autowired
    private FriendRepository friendRepository;

    @Autowired
    private EntityManager entityManager;

    private User createUser(String suffix) {
        User user = User.create(
                "login" + suffix,
                "encodedPassword",
                "nickname" + suffix,
                "email" + suffix + "@test.com",
                "0101234" + suffix
        );

        entityManager.persist(user);

        return user;
    }

    private Friend createFriend(User requester, User receiver) {
        return friendRepository.save(
                Friend.request(requester, receiver)
        );
    }

    private Friend createAcceptedFriend(User requester, User receiver) {
        Friend friend = Friend.request(requester, receiver);
        friend.acceptFriend();

        return friendRepository.save(friend);
    }

    // --------------------------------------------------
    // findAcceptedFriends
    // --------------------------------------------------

    @Test
    @DisplayName("요청자 또는 수신자가 사용자인 ACCEPTED 친구 목록을 조회한다")
    void findAcceptedFriends() {
        // given
        User user = createUser("001");
        User requester = createUser("002");
        User receiver = createUser("003");
        User pendingUser = createUser("004");

        User unrelatedUser1 = createUser("039");
        User unrelatedUser2 = createUser("040");

        Friend receivedFriend = createAcceptedFriend(requester, user);
        Friend sentFriend = createAcceptedFriend(user, receiver);

        // 사용자와 무관한 ACCEPTED 관계
        Friend unrelatedFriend =
                createAcceptedFriend(unrelatedUser1, unrelatedUser2);

        // 사용자와 관련 있지만 PENDING 상태인 관계
        createFriend(user, pendingUser);

        entityManager.flush();
        entityManager.clear();

        // when
        List<Friend> result = friendRepository.findAcceptedFriends(
                user.getId(),
                FriendStatus.ACCEPTED
        );

        // then
        assertThat(result)
                .hasSize(2)
                .extracting(Friend::getId)
                .containsExactlyInAnyOrder(
                        receivedFriend.getId(),
                        sentFriend.getId()
                )
                .doesNotContain(unrelatedFriend.getId());

        assertThat(result)
                .allSatisfy(friend -> {
                    assertThat(friend.getFriendStatus())
                            .isEqualTo(FriendStatus.ACCEPTED);

                    assertThat(
                            friend.getRequester().getId().equals(user.getId())
                                    || friend.getReceiver().getId().equals(user.getId())
                    ).isTrue();

                    assertThat(Hibernate.isInitialized(friend.getRequester()))
                            .isTrue();

                    assertThat(Hibernate.isInitialized(friend.getReceiver()))
                            .isTrue();
                });
    }

    // --------------------------------------------------
    // findReceivedRequests
    // --------------------------------------------------

    @Test
    @DisplayName("사용자가 받은 PENDING 친구 요청 목록을 조회한다")
    void findReceivedRequests() {
        // given
        User user = createUser("005");
        User requester1 = createUser("006");
        User requester2 = createUser("007");
        User otherUser = createUser("008");

        Friend receivedFriend1 = createFriend(requester1, user);
        Friend receivedFriend2 = createFriend(requester2, user);

        createFriend(user, otherUser);
        createAcceptedFriend(otherUser, requester1);

        entityManager.flush();
        entityManager.clear();

        // when
        List<Friend> result = friendRepository.findReceivedRequests(
                user.getId(),
                FriendStatus.PENDING
        );

        // then
        assertThat(result)
                .hasSize(2)
                .extracting(Friend::getId)
                .containsExactlyInAnyOrder(
                        receivedFriend1.getId(),
                        receivedFriend2.getId()
                );

        assertThat(result)
                .allSatisfy(friend -> {
                    assertThat(friend.getReceiver().getId())
                            .isEqualTo(user.getId());

                    assertThat(friend.getFriendStatus())
                            .isEqualTo(FriendStatus.PENDING);

                    assertThat(Hibernate.isInitialized(friend.getRequester()))
                            .isTrue();

                    assertThat(Hibernate.isInitialized(friend.getReceiver()))
                            .isTrue();
                });
    }

    // --------------------------------------------------
    // findSentRequests
    // --------------------------------------------------

    @Test
    @DisplayName("사용자가 보낸 PENDING 친구 요청 목록을 조회한다")
    void findSentRequests() {
        // given
        User user = createUser("009");
        User receiver1 = createUser("010");
        User receiver2 = createUser("011");
        User otherUser = createUser("012");

        Friend sentFriend1 = createFriend(user, receiver1);
        Friend sentFriend2 = createFriend(user, receiver2);

        createFriend(otherUser, user);
        createAcceptedFriend(user, otherUser);

        entityManager.flush();
        entityManager.clear();

        // when
        List<Friend> result = friendRepository.findSentRequests(
                user.getId(),
                FriendStatus.PENDING
        );

        // then
        assertThat(result)
                .hasSize(2)
                .extracting(Friend::getId)
                .containsExactlyInAnyOrder(
                        sentFriend1.getId(),
                        sentFriend2.getId()
                );

        assertThat(result)
                .allSatisfy(friend -> {
                    assertThat(friend.getRequester().getId())
                            .isEqualTo(user.getId());

                    assertThat(friend.getFriendStatus())
                            .isEqualTo(FriendStatus.PENDING);

                    assertThat(Hibernate.isInitialized(friend.getRequester()))
                            .isTrue();

                    assertThat(Hibernate.isInitialized(friend.getReceiver()))
                            .isTrue();
                });
    }

    // --------------------------------------------------
    // findByIdWithUsers
    // --------------------------------------------------

    @Test
    @DisplayName("친구 관계 ID로 조회하면서 요청자와 수신자를 함께 조회한다")
    void findByIdWithUsers() {
        // given
        User requester = createUser("013");
        User receiver = createUser("014");

        Friend friend = createFriend(requester, receiver);

        entityManager.flush();
        entityManager.clear();

        // when
        Optional<Friend> result =
                friendRepository.findByIdWithUsers(friend.getId());

        // then
        assertThat(result).isPresent();

        Friend foundFriend = result.get();

        assertThat(foundFriend.getId())
                .isEqualTo(friend.getId());

        assertThat(foundFriend.getRequester().getId())
                .isEqualTo(requester.getId());

        assertThat(foundFriend.getReceiver().getId())
                .isEqualTo(receiver.getId());

        assertThat(Hibernate.isInitialized(foundFriend.getRequester()))
                .isTrue();

        assertThat(Hibernate.isInitialized(foundFriend.getReceiver()))
                .isTrue();
    }

    @Test
    @DisplayName("존재하지 않는 친구 관계 ID로 조회하면 빈 Optional을 반환한다")
    void findByIdWithUsersWhenNotExists() {
        // given
        Long invalidFriendId = 999999L;

        // when
        Optional<Friend> result =
                friendRepository.findByIdWithUsers(invalidFriendId);

        // then
        assertThat(result).isEmpty();
    }

    // --------------------------------------------------
    // findRelation
    // --------------------------------------------------

    @Test
    @DisplayName("두 사용자 사이의 친구 관계를 조회한다")
    void findRelation() {
        // given
        User requester = createUser("015");
        User receiver = createUser("016");
        User otherUser = createUser("041");

        Friend friend = createFriend(requester, receiver);

        // requester만 겹치는 관계
        Friend requesterOverlap =
                createFriend(requester, otherUser);

        // receiver만 겹치는 관계
        Friend receiverOverlap =
                createFriend(otherUser, receiver);

        entityManager.flush();
        entityManager.clear();

        // when
        Optional<Friend> result = friendRepository.findRelation(
                requester.getId(),
                receiver.getId()
        );

        // then
        assertThat(result).isPresent();

        assertThat(result.get().getId())
                .isEqualTo(friend.getId())
                .isNotEqualTo(requesterOverlap.getId())
                .isNotEqualTo(receiverOverlap.getId());
    }

    @Test
    @DisplayName("사용자 ID 순서를 반대로 전달해도 친구 관계를 조회한다")
    void findRelationWhenUserOrderReversed() {
        // given
        User requester = createUser("017");
        User receiver = createUser("018");
        User otherUser = createUser("042");

        Friend friend = createFriend(requester, receiver);

        // requester만 겹치는 관계
        Friend requesterOverlap =
                createFriend(requester, otherUser);

        // receiver만 겹치는 관계
        Friend receiverOverlap =
                createFriend(otherUser, receiver);

        entityManager.flush();
        entityManager.clear();

        // when
        Optional<Friend> result = friendRepository.findRelation(
                receiver.getId(),
                requester.getId()
        );

        // then
        assertThat(result).isPresent();

        assertThat(result.get().getId())
                .isEqualTo(friend.getId())
                .isNotEqualTo(requesterOverlap.getId())
                .isNotEqualTo(receiverOverlap.getId());
    }

    @Test
    @DisplayName("두 사용자 사이에 친구 관계가 없으면 빈 Optional을 반환한다")
    void findRelationWhenNotExists() {
        // given
        User user1 = createUser("019");
        User user2 = createUser("020");

        // when
        Optional<Friend> result = friendRepository.findRelation(
                user1.getId(),
                user2.getId()
        );

        // then
        assertThat(result).isEmpty();
    }

    // --------------------------------------------------
    // existsAcceptedFriend
    // --------------------------------------------------

    @Test
    @DisplayName("두 사용자 사이에 ACCEPTED 친구 관계가 존재하면 true를 반환한다")
    void existsAcceptedFriend() {
        // given
        User requester = createUser("021");
        User receiver = createUser("022");
        User otherUser = createUser("043");

        createAcceptedFriend(requester, receiver);

        // requester만 겹치는 ACCEPTED 관계
        createAcceptedFriend(requester, otherUser);

        // receiver만 겹치는 ACCEPTED 관계
        createAcceptedFriend(otherUser, receiver);

        entityManager.flush();

        // when
        boolean result = friendRepository.existsAcceptedFriend(
                requester.getId(),
                receiver.getId(),
                FriendStatus.ACCEPTED
        );

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("사용자 ID 순서를 반대로 전달해도 ACCEPTED 친구 관계를 확인한다")
    void existsAcceptedFriendWhenUserOrderReversed() {
        // given
        User requester = createUser("023");
        User receiver = createUser("024");
        User otherUser = createUser("044");

        createAcceptedFriend(requester, receiver);

        // requester만 겹치는 ACCEPTED 관계
        createAcceptedFriend(requester, otherUser);

        // receiver만 겹치는 ACCEPTED 관계
        createAcceptedFriend(otherUser, receiver);

        entityManager.flush();

        // when
        boolean result = friendRepository.existsAcceptedFriend(
                receiver.getId(),
                requester.getId(),
                FriendStatus.ACCEPTED
        );

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("친구 요청 상태가 PENDING이면 ACCEPTED 친구 관계로 확인되지 않는다")
    void existsAcceptedFriendWhenPending() {
        // given
        User requester = createUser("025");
        User receiver = createUser("026");

        createFriend(requester, receiver);

        entityManager.flush();

        // when
        boolean result = friendRepository.existsAcceptedFriend(
                requester.getId(),
                receiver.getId(),
                FriendStatus.ACCEPTED
        );

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("친구 관계가 존재하지 않으면 false를 반환한다")
    void existsAcceptedFriendWhenNotExists() {
        // given
        User user1 = createUser("027");
        User user2 = createUser("028");

        // when
        boolean result = friendRepository.existsAcceptedFriend(
                user1.getId(),
                user2.getId(),
                FriendStatus.ACCEPTED
        );

        // then
        assertThat(result).isFalse();
    }

    // --------------------------------------------------
    // countAcceptedFriends
    // --------------------------------------------------

    @Test
    @DisplayName("사용자 ID 목록 중 ACCEPTED 친구 관계의 개수를 조회한다")
    void countAcceptedFriends() {
        // given
        User user = createUser("029");

        User friend1 = createUser("030");
        User friend2 = createUser("031");
        User pendingUser = createUser("032");
        User unrelatedUser = createUser("033");

        // user가 요청자 또는 수신자인 ACCEPTED 관계
        createAcceptedFriend(user, friend1);
        createAcceptedFriend(friend2, user);

        // PENDING 관계는 집계에서 제외
        createFriend(user, pendingUser);

        // 목록에 포함되지 않은 사용자의 관계
        createAcceptedFriend(user, unrelatedUser);

        List<Long> friendIds = List.of(
                friend1.getId(),
                friend2.getId(),
                pendingUser.getId()
        );

        entityManager.flush();

        // when
        long result = friendRepository.countAcceptedFriends(
                user.getId(),
                friendIds,
                FriendStatus.ACCEPTED
        );

        // then
        assertThat(result).isEqualTo(2L);
    }

    // --------------------------------------------------
    // deleteAllByUserId
    // --------------------------------------------------

    @Test
    @DisplayName("요청자 또는 수신자가 사용자인 친구 관계를 모두 삭제한다")
    void deleteAllByUserId() {
        // given
        User user = createUser("034");

        User requester = createUser("035");
        User receiver = createUser("036");
        User unrelatedUser1 = createUser("037");
        User unrelatedUser2 = createUser("038");

        Friend receivedFriend = createFriend(requester, user);
        Friend sentFriend = createFriend(user, receiver);

        Friend unrelatedFriend =
                createFriend(unrelatedUser1, unrelatedUser2);

        entityManager.flush();

        // when
        friendRepository.deleteAllByUserId(user.getId());

        entityManager.flush();
        entityManager.clear();

        // then
        List<Friend> result = friendRepository.findAll();

        assertThat(result)
                .hasSize(1)
                .extracting(Friend::getId)
                .containsExactly(unrelatedFriend.getId());

        assertThat(friendRepository.findById(receivedFriend.getId()))
                .isEmpty();

        assertThat(friendRepository.findById(sentFriend.getId()))
                .isEmpty();
    }
}