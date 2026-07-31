package com.example.schedulebook.domain.schedule.repository;

import com.example.schedulebook.domain.schedule.entity.ScheduleReminder;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ScheduleReminderRepository extends JpaRepository<ScheduleReminder, Long> {
    void deleteBySchedule_Id(Long id);

    @Query("SELECT r FROM ScheduleReminder r JOIN FETCH r.schedule s " +
            "JOIN FETCH s.user WHERE r.reminderTime <= :now AND r.sent = false")
    List<ScheduleReminder> findReminders(@Param("now") LocalDateTime now, Pageable pageable);
}
