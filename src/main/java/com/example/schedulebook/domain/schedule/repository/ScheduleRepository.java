package com.example.schedulebook.domain.schedule.repository;

import com.example.schedulebook.domain.schedule.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
    List<Schedule> findAllByUserIdAndScheduleDateBetween(
            Long userId,
            LocalDate startDate,
            LocalDate endDate
    );

    List<Schedule> findByUser_IdAndScheduleDate(Long userId, LocalDate scheduleDate);

    @Query("SELECT s FROM Schedule s JOIN FETCH s.user WHERE s.scheduleDate = :date " +
            "AND s.startTime = :time AND s.reminderSent = false")
    List<Schedule> findSchedulesForReminder(@Param("date") LocalDate date, @Param("time") LocalTime time);

    @Modifying
    @Query("UPDATE Schedule s SET s.commentCount = s.commentCount + 1 WHERE s.id = :scheduleId")
    void increaseCommentCount(@Param("scheduleId") Long scheduleId);

    @Modifying
    @Query("UPDATE Schedule s SET s.commentCount = s.commentCount - 1 WHERE s.id = :scheduleId AND s.commentCount > 0")
    void decreaseCommentCount(@Param("scheduleId") Long scheduleId);

    @Query("SELECT s FROM Schedule s JOIN FETCH s.user WHERE s.id = :scheduleId")
    Optional<Schedule> findWithOwner(@Param("scheduleId") Long scheduleId);

    @Query("SELECT s.commentCount FROM Schedule s WHERE s.id = :scheduleId")
    int findCommentCount(@Param("scheduleId") Long scheduleId);
}
