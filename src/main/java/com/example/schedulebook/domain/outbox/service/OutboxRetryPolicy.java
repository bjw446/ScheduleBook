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
        return Math.min(CommonConst.NEXT_RETRY_DELAY * (1L << retryCount), 3600L);
    }
}
