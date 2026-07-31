package com.example.schedulebook.domain.schedule.repository;

import com.example.schedulebook.domain.schedule.entity.ScheduleReminder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ScheduleReminderRepository extends JpaRepository<ScheduleReminder, Long> {
    void deleteBySchedule_Id(Long id);

    List<ScheduleReminder> findByReminderTimeAndSent(LocalDateTime now, boolean sent);
}
