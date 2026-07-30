package com.example.schedulebook.domain.notificationretry.service;

import com.example.schedulebook.domain.notificationretry.entity.ProcessedNotificationRetry;
import com.example.schedulebook.domain.notificationretry.repository.ProcessedNotificationRetryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProcessedNotificationRetryCreateService {
    private final ProcessedNotificationRetryRepository processedNotificationRetryRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ProcessedNotificationRetry create(Long outboxId, Long receiverId, String owner) {
        return processedNotificationRetryRepository.save(ProcessedNotificationRetry.create(
                outboxId,
                receiverId,
                owner
        ));
    }
}
