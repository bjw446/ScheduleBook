package com.example.schedulebook.domain.schedule.repository;

import com.example.schedulebook.domain.schedule.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
    List<Schedule> findAllByUserIdAndScheduleDateBetween(
            Long userId,
            LocalDate startDate,
            LocalDate endDate
    );

    List<Schedule> findByUser_IdAndScheduleDate(Long userId, LocalDate scheduleDate);

    @Query("SELECT s FROM Schedule s JOIN FETCH s.user WHERE s.scheduleDate = :date " +
            "AND s.startTime >= :from AND s.startTime < :to AND s.reminderSent = false")
    List<Schedule> findSchedulesForReminder(@Param("date") LocalDate date,
                                            @Param("from") LocalTime from,
                                            @Param("to") LocalTime to
    );
}
