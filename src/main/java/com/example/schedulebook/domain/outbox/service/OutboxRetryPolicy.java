package com.example.schedulebook.domain.outbox.service;

import com.example.schedulebook.common.consts.CommonConst;
import com.example.schedulebook.domain.outbox.dto.RetryDecision;
import com.example.schedulebook.domain.outbox.enums.OutboxStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class OutboxRetryPolicy {
    public RetryDecision decide(int retryCount) {
        if (retryCount >= CommonConst.MAX_RETRY) {
            return new RetryDecision(OutboxStatus.DEAD, null);
        }

        return new RetryDecision(OutboxStatus.FAILED, LocalDateTime.now().plusSeconds(nextDelaySeconds(retryCount)));
    }

    private long nextDelaySeconds(int retryCount) {
        return switch (retryCount) {
            case 0 -> 60;
            case 1 -> 300;
            case 2 -> 900;
            case 3 -> 1800;
            default -> 3600;
        };
    }
}
