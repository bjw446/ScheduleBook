package com.example.schedulebook.domain.outbox.service;

import com.example.schedulebook.domain.outbox.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class OutboxCleanupService {
    private final OutboxRepository outboxRepository;

    public int cleanup(int days) {
        LocalDateTime target = LocalDateTime.now().minusDays(days);

        return outboxRepository.deleteSuccessBefore(target);
    }
}