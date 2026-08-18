package com.example.schedulebook.domain.deadletter.repository;

import com.example.schedulebook.domain.deadletter.entity.DeadLetterQueue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface DeadLetterRepository extends JpaRepository<DeadLetterQueue, Long> {
    @Modifying
    @Query("UPDATE DeadLetterQueue d SET d.deadLetterStatus = " +
            "com.example.schedulebook.domain.deadletter.enums.DeadLetterStatus.RECOVERED," +
            "d.processingAt = NULL, d.claimToken = NULL " +
            "WHERE d.id = :deadLetterId AND d.claimToken = :claimToken AND d.deadLetterStatus = " +
            "com.example.schedulebook.domain.deadletter.enums.DeadLetterStatus.PROCESSING")
    int markRecovered(@Param("deadLetterId") Long deadLetterId, @Param("claimToken") String claimToken);

    @Modifying
    @Query("UPDATE DeadLetterQueue d SET d.deadLetterStatus = " +
            "com.example.schedulebook.domain.deadletter.enums.DeadLetterStatus.PROCESSING," +
            "d.processingAt = CURRENT_TIMESTAMP, d.claimToken = :claimToken " +
            "WHERE d.id = :deadLetterId AND d.deadLetterStatus = " +
            "com.example.schedulebook.domain.deadletter.enums.DeadLetterStatus.PENDING")
    int markProcessing(@Param("deadLetterId") Long deadLetterId, @Param("claimToken") String claimToken);

    @Modifying
    @Query("UPDATE DeadLetterQueue d SET d.deadLetterStatus = " +
            "com.example.schedulebook.domain.deadletter.enums.DeadLetterStatus.PENDING," +
            "d.processingAt = NULL, d.claimToken = NULL " +
            "WHERE d.id = :deadLetterId AND d.claimToken = :claimToken AND d.deadLetterStatus = " +
            "com.example.schedulebook.domain.deadletter.enums.DeadLetterStatus.PROCESSING")
    int markPending(@Param("deadLetterId") Long deadLetterId, @Param("claimToken") String claimToken);

    @Modifying
    @Query("UPDATE DeadLetterQueue d SET d.deadLetterStatus = " +
            "com.example.schedulebook.domain.deadletter.enums.DeadLetterStatus.PENDING, " +
            "d.processingAt = NULL, d.claimToken = NULL  WHERE d.deadLetterStatus = " +
            "com.example.schedulebook.domain.deadletter.enums.DeadLetterStatus.PROCESSING " +
            "AND d.processingAt < :expiredAt")
    int reclaimExpiredProcessing(@Param("expiredAt") LocalDateTime expiredAt);
}
