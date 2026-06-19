package com.example.schedulebook.domain.schedule.repository;

import com.example.schedulebook.domain.schedule.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
    List<Schedule> findAllByUserId(Long userId);

    List<Schedule> findAllByUserIdAndScheduleDateBetween(
            Long userId,
            LocalDate startDate,
            LocalDate endDate
    );

    List<Schedule> findByUserIdAndScheduleDate(Long userId, LocalDate scheduleDate);
}
