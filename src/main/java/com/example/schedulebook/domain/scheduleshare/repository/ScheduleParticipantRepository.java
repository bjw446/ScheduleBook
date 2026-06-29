package com.example.schedulebook.domain.scheduleshare.repository;

import com.example.schedulebook.domain.scheduleshare.entity.ScheduleParticipant;
import com.example.schedulebook.domain.scheduleshare.enums.AttendanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ScheduleParticipantRepository extends JpaRepository<ScheduleParticipant, Long> {
    Optional<ScheduleParticipant> findBySchedule_IdAndUser_Id(Long scheduleId, Long userId);

    List<ScheduleParticipant> findAllBySchedule_Id(Long scheduleId);

    @Query("SELECT sp.user.id FROM ScheduleParticipant sp WHERE sp.schedule.id = :scheduleId")
    List<Long> findParticipantIds(@Param("scheduleId") Long scheduleId);


    @Query("SELECT COUNT(sp) > 0 FROM ScheduleParticipant sp WHERE sp.schedule.id = :scheduleId " +
            "AND sp.user.id = :userId AND sp.attendanceStatus = :attendanceStatus")
    boolean existsAcceptedParticipant(
            @Param("scheduleId") Long scheduleId,
            @Param("userId") Long userId,
            @Param("attendanceStatus") AttendanceStatus attendanceStatus);

    boolean existsBySchedule_IdAndUser_Id(Long scheduleId, Long userId);
}
