package com.example.schedulebook.domain.schedule.repository;

import com.example.schedulebook.domain.schedule.entity.ScheduleReminder;
import com.example.schedulebook.domain.schedule.enums.ScheduleReminderStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ScheduleReminderRepository extends JpaRepository<ScheduleReminder, Long> {
    void deleteBySchedule_Id(Long id);

    @Query("SELECT r FROM ScheduleReminder r JOIN FETCH r.schedule s " +
            "JOIN FETCH s.user WHERE r.reminderTime <= :now AND r.scheduleReminderStatus = " +
            "com.example.schedulebook.domain.schedule.enums.ScheduleReminderStatus.PENDING " +
            "ORDER BY r.reminderTime ASC, r.id ASC")
    List<ScheduleReminder> findReminders(@Param("now") LocalDateTime now, Pageable pageable);

    @Modifying
    @Query("UPDATE ScheduleReminder r SET r.scheduleReminderStatus = " +
            "com.example.schedulebook.domain.schedule.enums.ScheduleReminderStatus.PROCESSING, " +
            "r.claimToken = :claimToken, r.claimedAt = :claimedAt " +
            "WHERE r.id = :scheduleReminderId AND r.scheduleReminderStatus = " +
            "com.example.schedulebook.domain.schedule.enums.ScheduleReminderStatus.PENDING")
    int markProcessing(@Param("scheduleReminderId") Long scheduleReminderId,
                       @Param("claimToken") String claimToken,
                       @Param("claimedAt") LocalDateTime claimedAt);

    @Modifying
    @Query("UPDATE ScheduleReminder r SET r.scheduleReminderStatus = " +
            "com.example.schedulebook.domain.schedule.enums.ScheduleReminderStatus.SENT, " +
            "r.claimToken = NULL, r.claimedAt = NULL " +
            "WHERE r.id = :scheduleReminderId AND r.scheduleReminderStatus = " +
            "com.example.schedulebook.domain.schedule.enums.ScheduleReminderStatus.PROCESSING " +
            "AND r.claimToken = :claimToken")
    int markSent(@Param("scheduleReminderId") Long scheduleReminderId, @Param("claimToken") String claimToken);

    @Modifying
    @Query("UPDATE ScheduleReminder r SET r.scheduleReminderStatus = " +
            "com.example.schedulebook.domain.schedule.enums.ScheduleReminderStatus.PENDING, " +
            "r.claimToken = NULL, r.claimedAt = NULL " +
            "WHERE r.id = :scheduleReminderId AND r.scheduleReminderStatus = " +
            "com.example.schedulebook.domain.schedule.enums.ScheduleReminderStatus.PROCESSING " +
            "AND r.claimToken = :claimToken")
    int markPending(@Param("scheduleReminderId") Long scheduleReminderId, @Param("claimToken") String claimToken);

    @Modifying
    @Query("UPDATE ScheduleReminder r SET r.scheduleReminderStatus = " +
            "com.example.schedulebook.domain.schedule.enums.ScheduleReminderStatus.PENDING, " +
            "r.claimToken = NULL, r.claimedAt = NULL WHERE r.scheduleReminderStatus = " +
            "com.example.schedulebook.domain.schedule.enums.ScheduleReminderStatus.PROCESSING " +
            "AND r.claimedAt < :threshold")
    int recoverStuckReminders(@Param("threshold") LocalDateTime threshold);

    long countByScheduleReminderStatus(ScheduleReminderStatus scheduleReminderStatus);
}
