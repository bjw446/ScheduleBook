package com.example.schedulebook.domain.schedule.repository;

import com.example.schedulebook.domain.schedule.entity.ScheduleParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ScheduleParticipantRepository extends JpaRepository<ScheduleParticipant, Long> {
    Optional<ScheduleParticipant> findBySchedule_IdAndUser_Id(Long scheduleId, Long userId);

    @Query("SELECT sp.user.id FROM ScheduleParticipant sp WHERE sp.schedule.id = :scheduleId")
    List<Long> findParticipantIds(@Param("scheduleId") Long scheduleId);

    boolean existsBySchedule_IdAndUser_Id(Long scheduleId, Long userId);

    @Query("SELECT sp FROM ScheduleParticipant sp JOIN FETCH sp.user JOIN FETCH sp.schedule s " +
            "JOIN FETCH s.user WHERE sp.schedule.id = :scheduleId")
    List<ScheduleParticipant> findParticipants(@Param("scheduleId") Long scheduleId);
}
