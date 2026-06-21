package com.example.schedulebook.domain.notification.repository;

import com.example.schedulebook.domain.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    @Query("SELECT n FROM Notification n WHERE n.receiver.id = :userId ORDER BY n.createdAt DESC")
    List<Notification> findAllByReceiverId(@Param("userId") Long userId);

    @Query("SELECT n FROM Notification n JOIN FETCH n.receiver WHERE n.id = :notificationId")
    Optional<Notification> findByIdWithReceiver(@Param("notificationId") Long notificationId);

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.receiver.id  = :userId AND n.isRead = false ")
    long countUnreadNotifications(@Param("userId") Long userId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.receiver.id = :userId AND n.isRead = false ")
    int readAllNotification(@Param("userId") Long userId);
}
