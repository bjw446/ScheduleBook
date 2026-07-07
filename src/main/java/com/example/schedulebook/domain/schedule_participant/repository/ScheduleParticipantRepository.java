package com.example.schedulebook.domain.schedule_participant.repository;

import com.example.schedulebook.domain.schedule_participant.entity.ScheduleParticipant;
import com.example.schedulebook.domain.schedule_participant.projection.ScheduleParticipantProjection;
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

    @Query("SELECT u.id AS userId, u.nickname AS nickname, CASE WHEN u.id = s.user.id " +
            "THEN TRUE ELSE FALSE END AS owner, sp.attendanceStatus AS attendanceStatus " +
            "FROM ScheduleParticipant sp JOIN sp.user u JOIN sp.schedule s " +
            "WHERE s.id = :scheduleId ORDER BY sp.id")
    List<ScheduleParticipantProjection> findParticipants(@Param("scheduleId") Long scheduleId);

    List<ScheduleParticipant> findAllBySchedule_Id(Long scheduleId);
}
