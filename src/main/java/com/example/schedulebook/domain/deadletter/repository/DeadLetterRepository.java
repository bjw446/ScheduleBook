package com.example.schedulebook.domain.deadletter.repository;

import com.example.schedulebook.domain.deadletter.entity.DeadLetterQueue;
import com.example.schedulebook.domain.deadletter.enums.DeadLetterStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface DeadLetterRepository extends JpaRepository<DeadLetterQueue, Long> {
    @Modifying
    @Query("UPDATE DeadLetterQueue d SET d.deadLetterStatus = " +
            "com.example.schedulebook.domain.deadletter.enums.DeadLetterStatus.RECOVERED," +
            "d.leaseUntil = NULL, d.claimToken = NULL " +
            "WHERE d.id = :deadLetterId AND d.claimToken = :claimToken AND d.deadLetterStatus = " +
            "com.example.schedulebook.domain.deadletter.enums.DeadLetterStatus.PROCESSING")
    int markRecovered(@Param("deadLetterId") Long deadLetterId, @Param("claimToken") String claimToken);

    @Modifying
    @Query("UPDATE DeadLetterQueue d SET d.deadLetterStatus = " +
            "com.example.schedulebook.domain.deadletter.enums.DeadLetterStatus.PROCESSING, " +
            "d.leaseUntil = :leaseUntil, d.claimToken = :claimToken " +
            "WHERE d.id = :deadLetterId AND ( d.deadLetterStatus = " +
            "com.example.schedulebook.domain.deadletter.enums.DeadLetterStatus.PENDING " +
            "OR ( d.deadLetterStatus = " +
            "com.example.schedulebook.domain.deadletter.enums.DeadLetterStatus.PROCESSING " +
            "AND d.leaseUntil <= CURRENT_TIMESTAMP))")
    int markProcessing(@Param("deadLetterId") Long deadLetterId,
                       @Param("claimToken") String claimToken,
                       @Param("leaseUntil") LocalDateTime leaseUntil);

    @Modifying
    @Query("UPDATE DeadLetterQueue d SET d.deadLetterStatus = " +
            "com.example.schedulebook.domain.deadletter.enums.DeadLetterStatus.PENDING," +
            "d.leaseUntil = NULL, d.claimToken = NULL " +
            "WHERE d.id = :deadLetterId AND d.claimToken = :claimToken AND d.deadLetterStatus = " +
            "com.example.schedulebook.domain.deadletter.enums.DeadLetterStatus.PROCESSING")
    int markPending(@Param("deadLetterId") Long deadLetterId, @Param("claimToken") String claimToken);

    // PROCESSING 상태의 lease 만료 시각을 기준으로 재시도 가능 상태로 회수
    // leaseUntil은 실제 처리 시작 시간이 아니라
    // PROCESSING 상태의 lease 만료 시각을 의미
    @Modifying
    @Query("UPDATE DeadLetterQueue d SET d.deadLetterStatus = " +
            "com.example.schedulebook.domain.deadletter.enums.DeadLetterStatus.PENDING, " +
            "d.leaseUntil = NULL, d.claimToken = NULL  WHERE d.deadLetterStatus = " +
            "com.example.schedulebook.domain.deadletter.enums.DeadLetterStatus.PROCESSING " +
            "AND d.leaseUntil <= CURRENT_TIMESTAMP")
    int reclaimExpiredProcessing();

    @Modifying
    @Query("UPDATE DeadLetterQueue d SET d.leaseUntil = :leaseUntil WHERE d.id = :deadLetterId " +
            "AND d.claimToken = :claimToken AND d.deadLetterStatus = " +
            "com.example.schedulebook.domain.deadletter.enums.DeadLetterStatus.PROCESSING")
    int renewLease(@Param("deadLetterId") Long deadLetterId,
                   @Param("claimToken") String claimToken,
                   @Param("leaseUntil") LocalDateTime leaseUntil);

    Optional<DeadLetterQueue> findByIdAndClaimTokenAndDeadLetterStatus(Long deadLetterId,
                                                                       String claimToken,
                                                                       DeadLetterStatus deadLetterStatus);
}
