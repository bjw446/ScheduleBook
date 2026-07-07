package com.example.schedulebook.domain.schedule_snapshot.repository;

import com.example.schedulebook.domain.schedule_snapshot.entity.ScheduleSnapshotHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ScheduleSnapshotHistoryRepository extends JpaRepository<ScheduleSnapshotHistory, Long> {
    @Query("SELECT h from ScheduleSnapshotHistory h WHERE h.chatMessage.id = :messageId " +
            "ORDER BY h.scheduleSnapshot.scheduleVersion DESC, h.id DESC")
    List<ScheduleSnapshotHistory> findAllByChatMessageId(@Param("messageId") Long messageId);

    @Query("SELECT h from ScheduleSnapshotHistory h WHERE h.chatMessage.id = :messageId " +
            "AND h.scheduleSnapshot.scheduleVersion = :version")
    Optional<ScheduleSnapshotHistory> findByChatMessageIdAndVersion(@Param("messageId") Long messageId, @Param("version") Long version);
}
