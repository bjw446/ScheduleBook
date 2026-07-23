package com.example.schedulebook.domain.outbox.repository;

import com.example.schedulebook.domain.outbox.entity.Outbox;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface OutboxRepository extends JpaRepository<Outbox, Long> {
    @Query("SELECT o FROM Outbox o WHERE o.status = com.example.schedulebook.domain.outbox.enums.OutboxStatus.PENDING " +
            "OR (o.status = com.example.schedulebook.domain.outbox.enums.OutboxStatus.FAILED " +
            "AND o.nextRetryAt <= :now) ORDER BY o.id")
    List<Outbox> findRetryTargets(@Param("now") LocalDateTime now, Pageable pageable);
}
