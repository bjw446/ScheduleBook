package com.example.schedulebook.domain.deadletter.repository;

import com.example.schedulebook.domain.deadletter.entity.DeadLetterQueue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DeadLetterRepository extends JpaRepository<DeadLetterQueue, Long> {
    @Modifying
    @Query("UPDATE DeadLetterQueue d SET d.deadLetterStatus = " +
            "com.example.schedulebook.domain.deadletter.enums.DeadLetterStatus.RECOVERED " +
            "WHERE d.id = :deadLetterId AND d.deadLetterStatus = " +
            "com.example.schedulebook.domain.deadletter.enums.DeadLetterStatus.PROCESSING")
    int markRecovered(@Param("deadLetterId") Long deadLetterId);

    @Modifying
    @Query("UPDATE DeadLetterQueue d SET d.deadLetterStatus = " +
            "com.example.schedulebook.domain.deadletter.enums.DeadLetterStatus.PROCESSING " +
            "WHERE d.id = :deadLetterId AND d.deadLetterStatus = " +
            "com.example.schedulebook.domain.deadletter.enums.DeadLetterStatus.PENDING")
    int markProcessing(@Param("deadLetterId") Long deadLetterId);

    @Modifying
    @Query("UPDATE DeadLetterQueue d SET d.deadLetterStatus = " +
            "com.example.schedulebook.domain.deadletter.enums.DeadLetterStatus.PENDING " +
            "WHERE d.id = :deadLetterId AND d.deadLetterStatus = " +
            "com.example.schedulebook.domain.deadletter.enums.DeadLetterStatus.PROCESSING")
    int markPending(@Param("deadLetterId") Long deadLetterId);
}
