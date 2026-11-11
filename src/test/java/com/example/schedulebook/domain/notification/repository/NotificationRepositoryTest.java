package com.example.schedulebook.domain.notification.repository;

import com.example.schedulebook.common.config.JpaAuditingConfig;
import com.example.schedulebook.domain.notification.entity.Notification;
import com.example.schedulebook.domain.notification.enums.NotificationType;
import com.example.schedulebook.domain.user.entity.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnitUtil;
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
class NotificationRepositoryTest {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    private static final PersistenceUnitUtil persistenceUnitUtil;

    static {
        persistenceUnitUtil = null;
    }

    private User createUser(String suffix) {
        return User.create(
                "login" + suffix,
                "encodedPassword",
                "nickname" + suffix,
                "email" + suffix + "@test.com",
                "0101234" + suffix
        );
    }

    private Notification createNotification(
            User receiver,
            NotificationType notificationType,
            String title,
            String content,
            Long targetId
    ) {
        return Notification.create(
                receiver,
                notificationType,
                title,
                content,
                targetId
        );
    }

    private void persist(Object... entities) {
        for (Object entity : entities) {
            entityManager.persist(entity);
        }
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    private PersistenceUnitUtil getPersistenceUnitUtil() {
        return entityManagerFactory.getPersistenceUnitUtil();
    }

    @Test
    @DisplayName("특정 사용자의 알림을 생성일 내림차순으로 조회한다")
    void findAllByReceiverIdWithCreatedAtDescendingOrder() {
        // given
        User receiver = createUser("1");
        User anotherUser = createUser("2");

        persist(receiver, anotherUser);

        Notification firstNotification = createNotification(
                receiver,
                NotificationType.FRIEND_REQUEST,
                "첫 번째 알림",
                "첫 번째 내용",
                1L
        );

        Notification secondNotification = createNotification(
                receiver,
                NotificationType.SCHEDULE_SHARED,
                "두 번째 알림",
                "두 번째 내용",
                2L
        );

        Notification anotherUserNotification = createNotification(
                anotherUser,
                NotificationType.COMMENT_REPLY,
                "다른 사용자 알림",
                "다른 사용자 내용",
                3L
        );

        persist(firstNotification, secondNotification, anotherUserNotification);
        flushAndClear();

        // 생성 순서와 createdAt 정렬 순서가 다르도록 DB 값을 변경한다.
        entityManager.createNativeQuery("""
                UPDATE notifications
                SET created_at = TIMESTAMP '2026-01-01 00:00:00'
                WHERE id = :id
                """)
                .setParameter("id", firstNotification.getId())
                .executeUpdate();

        entityManager.createNativeQuery("""
                UPDATE notifications
                SET created_at = TIMESTAMP '2026-01-02 00:00:00'
                WHERE id = :id
                """)
                .setParameter("id", secondNotification.getId())
                .executeUpdate();

        flushAndClear();

        // when
        List<Notification> notifications =
                notificationRepository.findAllByReceiverId(receiver.getId());

        // then
        assertThat(notifications)
                .hasSize(2)
                .extracting(Notification::getTitle)
                .containsExactly("두 번째 알림", "첫 번째 알림");
    }

    @Test
    @DisplayName("알림 ID로 조회하면 receiver가 함께 조회된다")
    void findByIdWithReceiver() {
        // given
        User receiver = createUser("1");
        persist(receiver);

        Notification notification = createNotification(
                receiver,
                NotificationType.FRIEND_REQUEST,
                "친구 요청",
                "친구 요청이 도착했습니다.",
                1L
        );

        persist(notification);
        flushAndClear();

        // when
        Optional<Notification> result =
                notificationRepository.findByIdWithReceiver(notification.getId());

        // then
        assertThat(result).isPresent();

        Notification foundNotification = result.get();

        assertThat(foundNotification.getId()).isEqualTo(notification.getId());
        assertThat(foundNotification.getTitle()).isEqualTo("친구 요청");

        assertThat(getPersistenceUnitUtil().isLoaded(foundNotification, "receiver"))
                .isTrue();

        assertThat(foundNotification.getReceiver().getId())
                .isEqualTo(receiver.getId());
    }

    @Test
    @DisplayName("존재하지 않는 알림 ID로 조회하면 빈 Optional을 반환한다")
    void findByIdWithReceiverWhenNotExists() {
        // given
        Long notExistsNotificationId = 9999L;

        // when
        Optional<Notification> result =
                notificationRepository.findByIdWithReceiver(notExistsNotificationId);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("특정 사용자의 읽지 않은 알림 개수를 조회한다")
    void countUnreadNotifications() {
        // given
        User receiver = createUser("1");
        User anotherUser = createUser("2");

        persist(receiver, anotherUser);

        Notification unreadNotification1 = createNotification(
                receiver,
                NotificationType.FRIEND_REQUEST,
                "첫 번째 알림",
                "첫 번째 내용",
                1L
        );

        Notification unreadNotification2 = createNotification(
                receiver,
                NotificationType.SCHEDULE_SHARED,
                "두 번째 알림",
                "두 번째 내용",
                2L
        );

        Notification readNotification = createNotification(
                receiver,
                NotificationType.COMMENT_REPLY,
                "읽은 알림",
                "읽은 알림 내용",
                3L
        );

        Notification anotherUserNotification = createNotification(
                anotherUser,
                NotificationType.FRIEND_REQUEST,
                "다른 사용자 알림",
                "다른 사용자 내용",
                4L
        );

        readNotification.read();

        persist(
                unreadNotification1,
                unreadNotification2,
                readNotification,
                anotherUserNotification
        );

        flushAndClear();

        // when
        long unreadCount =
                notificationRepository.countUnreadNotifications(receiver.getId());

        // then
        assertThat(unreadCount).isEqualTo(2);
    }

    @Test
    @DisplayName("특정 사용자의 읽지 않은 알림을 전체 읽음 처리한다")
    void readAllNotifications() {
        // given
        User receiver = createUser("1");
        User anotherUser = createUser("2");

        persist(receiver, anotherUser);

        Notification unreadNotification1 = createNotification(
                receiver,
                NotificationType.FRIEND_REQUEST,
                "첫 번째 알림",
                "첫 번째 내용",
                1L
        );

        Notification unreadNotification2 = createNotification(
                receiver,
                NotificationType.SCHEDULE_SHARED,
                "두 번째 알림",
                "두 번째 내용",
                2L
        );

        Notification readNotification = createNotification(
                receiver,
                NotificationType.COMMENT_REPLY,
                "이미 읽은 알림",
                "이미 읽은 내용",
                3L
        );

        Notification anotherUserNotification = createNotification(
                anotherUser,
                NotificationType.FRIEND_REQUEST,
                "다른 사용자 알림",
                "다른 사용자 내용",
                4L
        );

        readNotification.read();

        persist(
                unreadNotification1,
                unreadNotification2,
                readNotification,
                anotherUserNotification
        );

        flushAndClear();

        // when
        int updatedCount =
                notificationRepository.readAllNotifications(receiver.getId());

        // then
        assertThat(updatedCount).isEqualTo(2);

        Notification foundUnreadNotification1 =
                notificationRepository.findById(unreadNotification1.getId()).orElseThrow();

        Notification foundUnreadNotification2 =
                notificationRepository.findById(unreadNotification2.getId()).orElseThrow();

        Notification foundReadNotification =
                notificationRepository.findById(readNotification.getId()).orElseThrow();

        Notification foundAnotherUserNotification =
                notificationRepository.findById(anotherUserNotification.getId()).orElseThrow();

        assertThat(foundUnreadNotification1.isRead()).isTrue();
        assertThat(foundUnreadNotification2.isRead()).isTrue();

        assertThat(foundReadNotification.isRead()).isTrue();
        assertThat(foundAnotherUserNotification.isRead()).isFalse();
    }

    @Test
    @DisplayName("특정 사용자의 알림을 전체 소프트 삭제한다")
    void softDeleteAllByReceiverId() {
        // given
        User receiver = createUser("1");
        User anotherUser = createUser("2");

        persist(receiver, anotherUser);

        Notification notification1 = createNotification(
                receiver,
                NotificationType.FRIEND_REQUEST,
                "첫 번째 알림",
                "첫 번째 내용",
                1L
        );

        Notification notification2 = createNotification(
                receiver,
                NotificationType.SCHEDULE_SHARED,
                "두 번째 알림",
                "두 번째 내용",
                2L
        );

        Notification anotherUserNotification = createNotification(
                anotherUser,
                NotificationType.COMMENT_REPLY,
                "다른 사용자 알림",
                "다른 사용자 내용",
                3L
        );

        persist(notification1, notification2, anotherUserNotification);
        flushAndClear();

        // when
        notificationRepository.softDeleteAllByReceiverId(receiver.getId());

        // 벌크 UPDATE는 영속성 컨텍스트와 동기화되지 않으므로 초기화한다.
        entityManager.clear();

        // then
        Notification deletedNotification1 =
                notificationRepository.findById(notification1.getId()).orElseThrow();

        Notification deletedNotification2 =
                notificationRepository.findById(notification2.getId()).orElseThrow();

        Notification notDeletedNotification =
                notificationRepository.findById(anotherUserNotification.getId()).orElseThrow();

        // 대상 사용자의 알림은 deleted 플래그가 true로 변경되어야 한다.
        assertThat(deletedNotification1.isDeleted()).isTrue();
        assertThat(deletedNotification2.isDeleted()).isTrue();

        // 대상 사용자의 알림은 deletedAt도 설정되어야 한다.
        assertThat(deletedNotification1.getDeletedAt()).isNotNull();
        assertThat(deletedNotification2.getDeletedAt()).isNotNull();

        // 다른 사용자의 알림은 삭제되지 않아야 한다.
        assertThat(notDeletedNotification.isDeleted()).isFalse();
        assertThat(notDeletedNotification.getDeletedAt()).isNull();
    }

    @Test
    @DisplayName("동일한 사용자와 대상 ID 및 알림 유형을 가진 알림이 존재하는지 확인한다")
    void existsNotification() {
        // given
        User receiver = createUser("1");
        User anotherUser = createUser("2");

        persist(receiver, anotherUser);

        Notification notification = createNotification(
                receiver,
                NotificationType.SCHEDULE_REMINDER,
                "일정 알림",
                "일정이 시작되었습니다.",
                100L
        );

        persist(notification);
        flushAndClear();

        // when
        boolean exists = notificationRepository.existsNotification(
                receiver.getId(),
                100L,
                NotificationType.SCHEDULE_REMINDER
        );

        // then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("대상 ID가 다르면 동일한 알림이 존재하지 않는 것으로 판단한다")
    void existsNotificationWhenTargetIdIsDifferent() {
        // given
        User receiver = createUser("1");
        persist(receiver);

        Notification notification = createNotification(
                receiver,
                NotificationType.SCHEDULE_REMINDER,
                "일정 알림",
                "일정이 시작되었습니다.",
                100L
        );

        persist(notification);
        flushAndClear();

        // when
        boolean exists = notificationRepository.existsNotification(
                receiver.getId(),
                200L,
                NotificationType.SCHEDULE_REMINDER
        );

        // then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("알림 유형이 다르면 동일한 알림이 존재하지 않는 것으로 판단한다")
    void existsNotificationWhenNotificationTypeIsDifferent() {
        // given
        User receiver = createUser("1");
        persist(receiver);

        Notification notification = createNotification(
                receiver,
                NotificationType.SCHEDULE_REMINDER,
                "일정 알림",
                "일정이 시작되었습니다.",
                100L
        );

        persist(notification);
        flushAndClear();

        // when
        boolean exists = notificationRepository.existsNotification(
                receiver.getId(),
                100L,
                NotificationType.FRIEND_REQUEST
        );

        // then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("수신자가 다르면 동일한 알림이 존재하지 않는 것으로 판단한다")
    void existsNotificationWhenReceiverIsDifferent() {
        // given
        User receiver = createUser("1");
        User anotherUser = createUser("2");

        persist(receiver, anotherUser);

        Notification notification = createNotification(
                receiver,
                NotificationType.SCHEDULE_REMINDER,
                "일정 알림",
                "일정이 시작되었습니다.",
                100L
        );

        persist(notification);
        flushAndClear();

        // when
        boolean exists = notificationRepository.existsNotification(
                anotherUser.getId(),
                100L,
                NotificationType.SCHEDULE_REMINDER
        );

        // then
        assertThat(exists).isFalse();
    }
}