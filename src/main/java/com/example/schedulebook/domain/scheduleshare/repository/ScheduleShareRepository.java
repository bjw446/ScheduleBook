package com.example.schedulebook.domain.scheduleshare.repository;

import com.example.schedulebook.domain.scheduleshare.entity.ScheduleShare;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ScheduleShareRepository extends JpaRepository <ScheduleShare, Long> {
    @Query("SELECT ss FROM ScheduleShare ss WHERE ss.schedule.id = :scheduleId AND ss.sharedUser.id = :sharedUserId")
    Optional<ScheduleShare> findRelation(Long scheduleId, Long sharedUserId);

    @Query("SELECT ss FROM ScheduleShare ss JOIN FETCH ss.schedule s JOIN FETCH s.user WHERE ss.sharedUser.id = :userId AND ss.scheduleShareStatus = 'ACTIVE' ")
    List<ScheduleShare> findSharedSchedules(Long userId);

    @Query("SELECT ss FROM ScheduleShare ss JOIN FETCH ss.schedule s JOIN FETCH s.user WHERE ss.id = :shareId")
    Optional<ScheduleShare> findByIdWithSchedule(Long shareId);

    @Query("SELECT ss FROM ScheduleShare ss JOIN FETCH ss.schedule s JOIN FETCH s.user WHERE ss.id = :shareId AND ss.scheduleShareStatus = 'ACTIVE' ")
    Optional<ScheduleShare> findActiveShareDetail(Long shareId);

    @Query("SELECT ss FROM ScheduleShare ss JOIN FETCH ss.schedule s JOIN FETCH ss.sharedUser WHERE s.user.id = :ownerId AND ss.scheduleShareStatus = 'ACTIVE' ")
    List<ScheduleShare> findOwnedShares(Long ownerId);

    @Query("SELECT ss FROM ScheduleShare ss JOIN FETCH ss.schedule s JOIN FETCH s.user JOIN FETCH ss.sharedUser WHERE ss.id = :shareId AND ss.scheduleShareStatus = 'ACTIVE' ")
    Optional<ScheduleShare> findOwnedShareDetail(Long shareId);
}
