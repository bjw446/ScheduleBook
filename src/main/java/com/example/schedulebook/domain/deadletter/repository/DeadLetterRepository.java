package com.example.schedulebook.domain.deadletter.repository;

import com.example.schedulebook.domain.deadletter.entity.DeadLetterQueue;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeadLetterRepository extends JpaRepository<DeadLetterQueue, Long> {
    Page<DeadLetterQueue> findAllPages(Pageable pageable);
}
