package com.example.schedulebook.domain.schedule_share.repository;

import com.example.schedulebook.domain.schedule_share.entity.ScheduleShare;
import com.example.schedulebook.domain.schedule_share.enums.ScheduleShareStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ScheduleShareRepository extends JpaRepository <ScheduleShare, Long> {
    @Query("SELECT ss FROM ScheduleShare ss WHERE ss.schedule.id = :scheduleId AND ss.sharedUser.id = :sharedUserId")
    Optional<ScheduleShare> findRelation(@Param("scheduleId") Long scheduleId, @Param("sharedUserId") Long sharedUserId);

    @Query("SELECT ss FROM ScheduleShare ss JOIN FETCH ss.schedule s JOIN FETCH s.user " +
            "WHERE ss.sharedUser.id = :userId AND ss.scheduleShareStatus = :status ")
    List<ScheduleShare> findSharedSchedules(@Param("userId") Long userId, @Param("status") ScheduleShareStatus status);

    @Query("SELECT ss FROM ScheduleShare ss JOIN FETCH ss.schedule s JOIN FETCH s.user WHERE ss.id = :shareId")
    Optional<ScheduleShare> findByIdWithSchedule(@Param("shareId") Long shareId);

    @Query("SELECT ss FROM ScheduleShare ss JOIN FETCH ss.schedule s JOIN FETCH s.user " +
            "WHERE ss.id = :shareId AND ss.scheduleShareStatus = :status ")
    Optional<ScheduleShare> findActiveShareDetail(@Param("shareId") Long shareId, @Param("status") ScheduleShareStatus status);

    @Query("SELECT ss FROM ScheduleShare ss JOIN FETCH ss.schedule s JOIN FETCH ss.sharedUser " +
            "WHERE s.user.id = :ownerId AND ss.scheduleShareStatus = :status ")
    List<ScheduleShare> findOwnedShares(@Param("ownerId") Long ownerId, @Param("status") ScheduleShareStatus status);

    @Query("SELECT ss FROM ScheduleShare ss JOIN FETCH ss.schedule s JOIN FETCH s.user " +
            "JOIN FETCH ss.sharedUser WHERE ss.id = :shareId AND ss.scheduleShareStatus = :status ")
    Optional<ScheduleShare> findOwnedShareDetail(@Param("shareId") Long shareId, @Param("status") ScheduleShareStatus status);

    @Query("SELECT ss FROM ScheduleShare ss WHERE ss.schedule.id = :scheduleId " +
            "AND ss.sharedUser.id = :userId AND ss.scheduleShareStatus = :status")
    Optional<ScheduleShare> findActiveRelation(
            @Param("scheduleId") Long scheduleId,
            @Param("userId") Long userId,
            @Param("status") ScheduleShareStatus status
    );

    boolean existsBySchedule_IdAndSharedUser_IdAndScheduleShareStatus(Long scheduleId, Long sharedUserId, ScheduleShareStatus status);

    List<ScheduleShare> findAllBySchedule_Id(Long scheduleId);
}
